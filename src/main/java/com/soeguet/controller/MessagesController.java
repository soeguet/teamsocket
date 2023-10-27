package com.soeguet.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soeguet.controller.interfaces.MessagesControllerInterface;
import com.soeguet.database.interfaces.DatabaseConnectionController;
import com.soeguet.model.UserInteraction;
import com.soeguet.model.dtos.DatabaseResult;
import com.soeguet.model.dtos.ReactionToSocketDTO;
import com.soeguet.model.dtos.UpdatedReactionModelDTO;
import com.soeguet.model.jackson.BaseModel;
import com.soeguet.model.jackson.MessageModel;
import com.soeguet.model.jackson.PictureModel;
import com.soeguet.util.MessageTypes;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagesController implements MessagesControllerInterface {

    private final Logger logger = Logger.getLogger(MessagesController.class.getName());
    private final DatabaseConnectionController databaseConnection;
    private final ObjectMapper mapper;

    public MessagesController(final DatabaseConnectionController databaseConnection) {

        this.databaseConnection = databaseConnection;
        mapper = new ObjectMapper();
    }

    /**
     Retrieves the last 100 messages from the database and serializes them to JSON.

     @return a Deque of JSON strings representing the serialized messages

     @throws RuntimeException
     if there is an error retrieving or parsing the messages
     */
    @Override
    public Deque<String> retrieveLast100Messages() {

        Deque<DatabaseResult> messageQueue = databaseConnection.getAllFromDatabase();
        final Deque<String> jsonQueue = new ArrayDeque<>();

        if (messageQueue.isEmpty()) {return jsonQueue;}

        do {

            final DatabaseResult databaseRow = messageQueue.poll();

            final BaseModel baseModel = deserializeBaseModel(databaseRow.message());
            baseModel.setId(databaseRow.id());

            try {

                if (baseModel instanceof PictureModel pictureModel) {

                    pictureModel.setPicture(databaseRow.image());
                    String jsonString = mapper.writeValueAsString(pictureModel);
                    jsonQueue.add(jsonString);

                } else {

                    String jsonString = mapper.writeValueAsString(baseModel);
                    jsonQueue.add(jsonString);
                }

            } catch (JsonProcessingException e) {

                throw new RuntimeException(e);
            }

        } while (!messageQueue.isEmpty());

        //additional information for the new client
        jsonQueue.add("__startup__end__");
        jsonQueue.add("welcome to the server");

        return jsonQueue;
    }

    @Override
    public String onMessageFromClient(final String message) {

        final BaseModel baseModel = deserializeBaseModel(message);

        if (baseModel instanceof MessageModel messageModel) {

            switch (messageModel.getMessageType()) {

                case MessageTypes.DELETED, MessageTypes.INTERACTED, MessageTypes.EDITED -> {

                    databaseConnection.replaceInDatabase(messageModel.getId(), message);

                    return databaseConnection.retrieveUpdatedEntry(messageModel.getId());
                }

                //new message
                default -> {

                    databaseConnection.saveToDatabase(message);

                    return initializeRetrievalOfLastMessageFromDatabase();
                }
            }

        } else if (baseModel instanceof PictureModel) {

            processAndInitializePersistenceOfPictureMessage(message);
            return initializeRetrievalOfLastMessageFromDatabase();
        }

        return "";
    }

    @Override
    public boolean checkByteArrayForReaction(final byte[] array) {

        //TODO implement this properly

        JsonNode jsonNode;

        try {

            jsonNode = mapper.readTree(array);

            return jsonNode.has("reactionName");

        } catch (IOException e) {

            logger.log(Level.SEVERE, "MessageController > checkByteArrayForReaction", e);
            logger.log(Level.SEVERE, "could not parse byte array to JsonNode, might be something else", e.getMessage());

            return false;
        }
    }

    @Override
    public UpdatedReactionModelDTO updateDatabaseEntryWithReaction(final byte[] byteData) {

        ReactionToSocketDTO reactionToSocketDTO;

        try {

            reactionToSocketDTO = mapper.readValue(byteData, ReactionToSocketDTO.class);

        } catch (IOException e) {

            logger.log(Level.SEVERE, "MessageController > checkByteArrayForReaction", e);
            logger.log(Level.SEVERE, "could not parse byte array to ReactionToSocketDTO, might be something else", e.getMessage());

            throw new RuntimeException(e);
        }

        //fetch entry
        String databaseEntry = databaseConnection.getMessageFromDatabase(reactionToSocketDTO.messageId());

        //deserialize entry
        BaseModel baseModel = deserializeBaseModel(databaseEntry);

        baseModel.setMessageType(MessageTypes.INTERACTED);

        //add reaction to entry
        UserInteraction userInteraction = new UserInteraction(reactionToSocketDTO.clientName(), reactionToSocketDTO.reactionName());
        baseModel.getUserInteractions().add(userInteraction);

        return new UpdatedReactionModelDTO(reactionToSocketDTO.messageId(), baseModel);
    }

    @Override
    public void replaceDatabaseEntryWithUpdatedModel(final UpdatedReactionModelDTO updatedModel) {

        String serializedAndUpdatedBaseModel;

        try {

            serializedAndUpdatedBaseModel = mapper.writeValueAsString(updatedModel.baseModel());

        } catch (JsonProcessingException e) {

            logger.log(Level.SEVERE, "MessageController > replaceDatabaseEntryWithUpdatedModel", e);
            logger.log(Level.SEVERE, "could not serialize updated model to JSON", e.getMessage());

            throw new RuntimeException(e);
        }

        //save to the database
        databaseConnection.replaceInDatabase(updatedModel.databaseId(), serializedAndUpdatedBaseModel);
    }

    @Override
    public void sendUpdatedBaseModelToAllClients(final Long databaseId) {

        final String updatedDatabaseEntry = databaseConnection.retrieveUpdatedEntry(databaseId);


    }

    /**
     Initializes the retrieval of the last message from the database and returns it as a JSON string.

     @return a JSON string representing the last message from the database

     @throws RuntimeException
     if there is an error retrieving or parsing the message
     */
    private String initializeRetrievalOfLastMessageFromDatabase() {

        final DatabaseResult databaseRow = databaseConnection.getLastFromDatabase();

        try {

            //deserialize message
            BaseModel model = deserializeBaseModel(databaseRow.message());
            model.setId(databaseRow.id());

            //if a picture, add it to the model
            if (model instanceof PictureModel pictureModel) {

                pictureModel.setPicture(databaseRow.image());
                return mapper.writeValueAsString(pictureModel);
            }

            //return model for broadcast
            return mapper.writeValueAsString(model);

        } catch (JsonProcessingException e) {

            throw new RuntimeException(e);

        }
    }

    /**
     Saves an image and its associated metadata to the database.

     @param message
     the JSON string representing the picture model

     @throws RuntimeException
     if there is an error saving the image to the database
     */
    private void processAndInitializePersistenceOfPictureMessage(String message) {

        //TODO explain this
        PictureModel pictureModel = parseJsonToPictureModel(message);

        //extract picture
        byte[] imageBytes = pictureModel.getPicture();

        //remove it afterward from the model (images are stored in its own table, need to extract it first)
        pictureModel.setPicture(null);

        //serialize the updated (image stripped) model to json
        String updatedPictureModelJson = serializePictureModelToJson(pictureModel);

        //save the message (message table is linked to the picture table) to the database and get id
        long messageId = databaseConnection.saveMessageWithoutPictureToDatabase(updatedPictureModelJson);

        //save the image to its own image database
        databaseConnection.saveExtractedImageToDatabaseInImageTable(messageId, imageBytes);
    }

    /**
     Parses a JSON string representation of a message into a PictureModel object.

     @param message
     the JSON string representing the message

     @return the parsed PictureModel object

     @throws RuntimeException
     if there is an error parsing the JSON
     */
    private PictureModel parseJsonToPictureModel(final String message) {

        try {

            return mapper.readValue(message, PictureModel.class);

        } catch (JsonProcessingException e) {

            logger.log(Level.SEVERE, "Error parsing JSON", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Serializes a PictureModel object to a JSON string.

     @param pictureModel
     the PictureModel object to serialize

     @return the JSON string representing the serialized PictureModel object

     @throws RuntimeException
     if there is an error parsing the JSON
     */
    private String serializePictureModelToJson(final PictureModel pictureModel) {

        try {

            return mapper.writeValueAsString(pictureModel);

        } catch (JsonProcessingException e) {

            logger.log(Level.SEVERE, "Error parsing JSON", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Deserializes the message into a BaseModel object.

     @param message
     the message to deserialize

     @return the deserialized BaseModel object

     @throws RuntimeException
     if there is an error while deserializing the message
     */
    private BaseModel deserializeBaseModel(final String message) {

        try {

            BaseModel deserializedModel = mapper.readValue(message, BaseModel.class);

            if (deserializedModel == null) {throw new RuntimeException("deserializedModel is null");}

            return deserializedModel;

        } catch (JsonProcessingException e) {

            throw new RuntimeException(e);
        }
    }
}