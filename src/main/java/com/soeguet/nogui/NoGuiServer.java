package com.soeguet.nogui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soeguet.model.jackson.BaseModel;
import com.soeguet.model.jackson.MessageModel;
import com.soeguet.model.jackson.PictureModel;
import com.soeguet.util.MessageTypes;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;

public class NoGuiServer extends WebSocketServer {

    private final String dbPath;

    private final Properties props;

    private final ObjectMapper mapper;

    private final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(NoGuiServer.class.getName());

    public NoGuiServer(String hostAddress, int port) {

        super(new InetSocketAddress(hostAddress, port));

        mapper = new ObjectMapper();

        dbPath = "jdbc:postgresql://localhost:5432/postgres";
        props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "postgres");
        props.setProperty("ssl", "false");

        this.initDatabase();

        this.run();
    }

    /**
     Initializes the database by creating the necessary table if it does not already exist.
     */
    private void initDatabase() {

        String createTableSql = "CREATE TABLE IF NOT EXISTS messages (id BIGSERIAL PRIMARY KEY, message VARCHAR(2255) NOT NULL);";
        String createTableSqlImages = "CREATE TABLE IF NOT EXISTS message_images (id BIGSERIAL PRIMARY KEY, message_id BIGINT REFERENCES messages(id), image_data bytea NOT NULL);";

        initiateDatabase("messages", createTableSql);
        initiateDatabase("message_images", createTableSqlImages);
    }

    /**
     Initializes the database by creating the necessary table if it does not already exist.

     @param sqlQuery the SQL statement to create the table
     */
    private void initiateDatabase(String databaseName, String sqlQuery) {

        if (!checkTableExists(databaseName)) {

            try {

                try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement()) {

                    stmt.executeUpdate(sqlQuery);
                    LOGGER.info("table created successfully - databasetabel: " + databaseName);

                }

            } catch (SQLException e) {

                LOGGER.log(Level.SEVERE, "Error initializing database", e);
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    /**
     Checks if the specified table exists in the database.

     @param tableName the name of the table to check

     @return true if the table exists, false otherwise
     */
    private boolean checkTableExists(String tableName) {

        try (Connection conn = DriverManager.getConnection(dbPath, props); ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {

            return rs.next();

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error checking if table exists", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     Called when a Ping message is received from a WebSocket connection.

     @param conn The WebSocket connection object representing the connection from which the Ping message was received.
     @param f    The Ping message frame data received from the client.
     */
    @Override
    public void onWebsocketPing(WebSocket conn, Framedata f) {

        super.onWebsocketPing(conn, f);
        broadcast("X".getBytes());
    }

    /**
     Called when a Pong frame is received from a WebSocket connection.

     @param conn The WebSocket connection object representing the connection from which the Pong frame was received.
     @param f    The Pong frame received from the client.
     */
    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {

        if (conn.getAttachment() == null) conn.setAttachment(new String(f.getPayloadData().array()));
    }

    /**
     Called when the WebSocket connection is closing.

     @param conn   The WebSocket connection object representing the connection being closed.
     @param code   The status code indicating the reason for closure.
     @param reason The reason for closure provided by the client or the server.
     @param remote Indicates whether the closure was initiated by the remote endpoint (client) or the local endpoint (server).
     */
    @Override
    public void onWebsocketClosing(WebSocket conn, int code, String reason, boolean remote) {

        super.onWebsocketClosing(conn, code, reason, remote);
    }

    /**
     Called when the WebSocket connection is closing.

     @param conn   The WebSocket connection object representing the connection being closed.
     @param code   The status code indicating the reason for closure.
     @param reason The reason for closure provided by the client or the server.
     @param remote Indicates whether the closure was initiated by the remote endpoint (client) or the local endpoint (server).
     */
    @Override
    public void onClosing(WebSocket conn, int code, String reason, boolean remote) {

        super.onClosing(conn, code, reason, remote);
    }

    /**
     Called when a new WebSocket connection is opened.

     @param conn      The WebSocket connection object representing the connection that was opened.
     @param handshake The ClientHandshake object representing the handshake information from the client.
     */
    @Override
    public synchronized void onOpen(WebSocket conn, ClientHandshake handshake) {

        LOGGER.info("User " + conn.getRemoteSocketAddress() + " just connected!");

        getAllFromDatabase(conn);
        sendMessage(conn, "welcome to the server!");

    }

    /**
     Retrieves the data from the database and sends it to the given WebSocket connection.

     @param connWebsocket The WebSocket connection object representing the connection.
     */
    private synchronized void getAllFromDatabase(WebSocket connWebsocket) {

        final String selectSql = "SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id LIMIT 100;";

        // actual query
        try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {

                final String id = fetchID(rs);
                final String message = getMessageFromResultSet(rs);

                final BaseModel messageModel = deserializeMessageModel(message);

                addImageToPictureModel(messageModel, rs);

                setIdIfAvailable(messageModel, id);

                sendMessage(connWebsocket, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageModel));
            }
        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error retrieving messages from database", e);
            throw new RuntimeException(e.getSQLState());

        } catch (JsonProcessingException e) {

            LOGGER.log(Level.SEVERE, "Error parsing JSON", e);
            throw new RuntimeException(e.getMessage());
        }

    }

    /**
     Sends a message to the specified WebSocket connection.

     @param connWebsocket The WebSocket connection object representing the connection.
     @param message       The message to be sent.
     */
    private synchronized void sendMessage(final WebSocket connWebsocket, final String message) {

        connWebsocket.send(message);
    }

    /**
     Retrieves the ID value from the ResultSet object.

     @param rs The ResultSet object containing the data.

     @return The ID value retrieved from the ResultSet.

     @throws SQLException If an error occurs during database access.
     */
    private String fetchID(final ResultSet rs) throws SQLException {

        return rs.getString("id");
    }

    /**
     Retrieves the "message" field value from the given ResultSet object.

     @param rs The ResultSet object from which to retrieve the message value.

     @return The message value as a string.

     @throws SQLException if a database access error occurs.
     */
    private String getMessageFromResultSet(final ResultSet rs) throws SQLException {

        return rs.getString("message");
    }

    /**
     Deserializes the given message into a BaseModel object.

     @param message The message to deserialize.

     @return The deserialized BaseModel object.

     @throws JsonProcessingException If an error occurs during the deserialization process.
     */
    private BaseModel deserializeMessageModel(final String message) throws JsonProcessingException {

        return mapper.readValue(message, BaseModel.class);
    }

    /**
     Adds an image to a PictureModel object.

     <p>
     This method checks if the given messageModel is an instance of PictureModel.
     If it is, it retrieves the image data from the given ResultSet and sets it as the picture of the PictureModel.
     </p>

     @param messageModel The BaseModel object to which the image will be added.
     @param rs           The ResultSet object containing the image data.

     @throws SQLException if there is an error retrieving the image data from the ResultSet.
     */
    private void addImageToPictureModel(final BaseModel messageModel, final ResultSet rs) throws SQLException {

        if (messageModel instanceof PictureModel) {

            byte[] imageBytes = rs.getBytes("image_data");
            ((PictureModel) messageModel).setPicture(imageBytes);
        }
    }

    /**
     Sets the ID of the message model if it is available. If the ID of the message model is null,
     the method sets it to the given ID value.

     @param messageModel The message model object to set the ID for.
     @param id           The ID value to set.
     */
    private void setIdIfAvailable(final BaseModel messageModel, final String id) {

        if (messageModel.getId() == null) {

            messageModel.setId(Long.parseLong(id));
        }
    }

    /**
     Called when the WebSocket connection is closed.

     @param conn   The WebSocket connection object representing the connection.
     @param code   The exit code indicating the reason for the closure.
     @param reason The additional information about the closure.
     @param remote Indicates whether the closure was initiated by the remote endpoint.
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        LOGGER.info("closed " + conn.getRemoteSocketAddress() + " " + conn.getAttachment() + " with exit code " + code + " additional info: " + reason);
    }

    /**
     Called when a message is received from a WebSocket connection.

     @param conn    The WebSocket connection object representing the connection from which the message was received.
     @param message The message received from the client as a string.
     */
    @Override
    public synchronized void onMessage(WebSocket conn, String message) {

        final BaseModel baseModel;
        try {
            baseModel = deserializeMessageModel(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (baseModel instanceof MessageModel) {

            switch (((MessageModel) baseModel).getMessageType()) {
                case MessageTypes.DELETED, MessageTypes.INTERACTED, MessageTypes.EDITED -> replaceInDatabase(baseModel.getId(), message);
                default -> {

                    saveToDatabase(message);
                    getLastFromDatabase();

                }
            }
        } else if (baseModel instanceof PictureModel) {

            saveImageToDatabase(message);
            getLastFromDatabase();

        }

        // remove is typing.. for all clients

        broadcast("X".getBytes());

    }

    /**
     Replaces a message in the database with the given ID and message.

     @param id      The ID of the message to replace.
     @param message The new message to replace the existing message with.
     */
    private synchronized void replaceInDatabase(Long id, String message) {

        // Prepare the UPDATE query
        String updateSql = "UPDATE messages SET message=? WHERE id=?";
        Connection connection;

        try {

            assert dbPath != null;
            connection = DriverManager.getConnection(dbPath, props);
            PreparedStatement updateStatement = connection.prepareStatement(updateSql);
            updateStatement.setString(1, message);
            updateStatement.setLong(2, id);

            int rowsUpdated = updateStatement.executeUpdate();

            if (rowsUpdated == 1) {

                // If one row was updated, retrieve the updated message from the database
                String selectSql = "SELECT * FROM messages WHERE id=?";
                PreparedStatement selectStatement = connection.prepareStatement(selectSql);
                selectStatement.setLong(1, id);
                ResultSet rs = selectStatement.executeQuery();

                if (rs.next()) broadcast(message);

            } else {

                // If no rows were updated, handle the error
                System.err.println("Failed to update message with ID " + id);
            }

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error updating message in database", e);
        }
    }

    /**
     Saves the given message to the database.

     @param message the message to be saved
     */
    private synchronized void saveToDatabase(String message) {

        String insertSql = "INSERT INTO messages (message) VALUES (?)";

        try {

            assert dbPath != null;

            try (Connection conn = DriverManager.getConnection(dbPath, props); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

                pstmt.setString(1, message);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error saving message to database", e);
        }
    }

    /**
     Retrieves the last message from the database and broadcasts it.

     <p>
     This method retrieves the last message from the `messages` table in the database. It uses a SQL query
     to select the last entry based on the `id` column. The retrieved message is then mapped to a `MessageModel`
     object using JSON deserialization. The `id` field of the `MessageModel` is set based on the retrieved `id`
     from the database. Finally, the broadcast method is called to broadcast the serialized `MessageModel` as a
     JSON string.
     </p>

     @throws RuntimeException if there is an error retrieving or processing the last message
     */
    private synchronized void getLastFromDatabase() {

        final String selectSql = "SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id DESC LIMIT 1;";

        try {

            try (Connection conn = DriverManager.getConnection(dbPath, props);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {

                while (rs.next()) {

                    final String id = fetchID(rs);
                    final String message = getMessageFromResultSet(rs);

                    BaseModel messageModel = deserializeMessageModel(message);

                    addImageToPictureModel(messageModel, rs);

                    messageModel.setId(Long.parseLong(id));

                    broadcast(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageModel));
                }
            }

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error retrieving last message from database", e);
            throw new RuntimeException(e.getSQLState());

        } catch (JsonProcessingException e) {

            LOGGER.log(Level.SEVERE, "Error parsing JSON", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     Saves an image and its associated metadata to the database.

     @param message the JSON string representing the picture model

     @throws RuntimeException if there is an error saving the image to the database
     */
    private void saveImageToDatabase(String message) {

        //preserve message id, link database entries
        long messageId;

        PictureModel pictureModel = parseJsonToPictureModel(message);

        //extract picture
        byte[] imageBytes = pictureModel.getPicture();

        //remove it afterward from the model
        pictureModel.setPicture(null);

        //serialize model to json
        String updatedPictureModelJson = serializePictureModelToJson(pictureModel);

        messageId = saveMessage(updatedPictureModelJson);

        saveImage(messageId, imageBytes);
    }

    /**
     Parses a JSON string representation of a message into a PictureModel object.

     @param message the JSON string representing the message

     @return the parsed PictureModel object

     @throws RuntimeException if there is an error parsing the JSON
     */
    private PictureModel parseJsonToPictureModel(final String message) {

        try {

            return mapper.readValue(message, PictureModel.class);

        } catch (JsonProcessingException e) {

            LOGGER.log(Level.SEVERE, "Error parsing JSON", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Serializes a PictureModel object to a JSON string.

     @param pictureModel the PictureModel object to serialize

     @return the JSON string representing the serialized PictureModel object

     @throws RuntimeException if there is an error parsing the JSON
     */
    private String serializePictureModelToJson(final PictureModel pictureModel) {

        try {

            return mapper.writeValueAsString(pictureModel);

        } catch (JsonProcessingException e) {

            LOGGER.log(Level.SEVERE, "Error parsing JSON", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Saves a message to the database and returns the generated message ID.

     <p>
     This method takes in a JSON string representing an updated `PictureModel` object and saves it to
     the `messages` table in the database. The JSON string is inserted into the `message` column of the table.
     The generated message ID is then returned.
     </p>

     @param updatedPictureModelJson an updated `PictureModel` JSON string

     @return the generated message ID after saving the message

     @throws RuntimeException if there is an error saving the message to the database
     */
    private long saveMessage(final String updatedPictureModelJson) {

        long messageId;
        final String insertMessageSql = "INSERT INTO messages (message) VALUES (?) RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbPath, props); PreparedStatement pstmt = conn.prepareStatement(insertMessageSql)) {

            pstmt.setString(1, updatedPictureModelJson);

            messageId = saveToDataBaseAndReturnId(pstmt);

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error saving message to database", e);
            throw new RuntimeException(e);
        }
        return messageId;
    }

    /**
     Saves an image associated with a message to the database.

     <p>
     This method takes a message ID and an array of image bytes and saves the image to the `message_images` table
     in the database. It uses a prepared statement with two parameters: the message ID and the image data. The image
     is then inserted into the database using an SQL INSERT statement. If there is an error saving the image, a
     RuntimeException is thrown with the details of the error.
     </p>

     @param messageId  the ID of the message to associate the image with
     @param imageBytes an array of bytes representing the image data

     @throws RuntimeException if there is an error saving the image to the database
     */
    private void saveImage(final long messageId, final byte[] imageBytes) {

        // insert image into database
        final String insertImageSql = "INSERT INTO message_images (message_id, image_data) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(dbPath, props); PreparedStatement pstmt = conn.prepareStatement(insertImageSql)) {

            saveImageToDatabase(pstmt, messageId, imageBytes);

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error saving image to database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Saves a message to the database and returns the generated ID.

     <p>
     This method saves a message to the specified database using a prepared statement.
     The generated ID is fetched from the result set and returned as a long value.
     If no ID is generated, a SQLException is thrown.
     </p>

     @param pstmt the PreparedStatement object for executing the SQL query

     @return the generated ID for the saved message

     @throws SQLException if there is an error executing the SQL query or fetching the generated ID
     */
    private long saveToDataBaseAndReturnId(final PreparedStatement pstmt) throws SQLException {

        // fetch generated ID
        try (ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {

                return rs.getLong(1);

            } else {

                LOGGER.log(Level.SEVERE, "Error saving message to database");
                throw new SQLException("no ID generated");
            }

        }
    }

    /**
     Saves an image to the database.

     <p>
     This method takes a prepared statement, a message ID, and image bytes as parameters.
     It sets the message ID and image bytes in the prepared statement using the setLong and setBytes methods
     respectively. Finally, it executes the update statement on the prepared statement to insert the image into
     the database.
     </p>

     @param pstmt      the prepared statement used to execute the update statement
     @param messageId  the ID of the message to which the image belongs
     @param imageBytes the byte array of the image to be saved

     @throws SQLException if there is an error executing the update statement
     */
    private void saveImageToDatabase(final PreparedStatement pstmt, final long messageId, final byte[] imageBytes) throws SQLException {

        pstmt.setLong(1, messageId);
        pstmt.setBytes(2, imageBytes);
        pstmt.executeUpdate();
    }

    /**
     Handles an error that occurs on a WebSocket connection.

     <p>
     This method logs the error message along with the remote socket address of the connection.
     </p>

     @param conn the WebSocket connection on which the error occurred
     @param ex   the exception that represents the error

     @throws RuntimeException if there is an error handling the WebSocket error
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {

        LOGGER.log(Level.SEVERE, "Error on connection " + conn.getRemoteSocketAddress() + ":" + ex);
        LOGGER.log(Level.SEVERE, "server might be running already!", ex);
        throw new RuntimeException(ex);
    }

    /**
     Handles the callback when the WebSocket server has successfully started.

     This method logs a message indicating that the server has started successfully,
     including the IP address and port on which it is listening.

     @see #getAddress()
     */
    @Override
    public void onStart() {

        LOGGER.info("server started successfully with ip " + this.getAddress().getHostString() + " and port " + this.getAddress().getPort() + "!");
    }

    /**
     Callback method invoked when a message is received by the WebSocket server.

     This method broadcasts the received message to all connected clients.

     @param conn    The WebSocket connection object.
     @param message The message received, as a ByteBuffer.
     */
    @Override
    public synchronized void onMessage(WebSocket conn, ByteBuffer message) {

        broadcast(((String) conn.getAttachment()).getBytes());

    }
}