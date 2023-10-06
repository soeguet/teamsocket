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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NoGuiServer extends WebSocketServer {

    private final Properties props;
    private final ObjectMapper mapper;
    private final Logger logger = Logger.getLogger(NoGuiServer.class.getName());
    private String dbPath;

    /**
     Constructs a NoGuiServer object with the given host address and port number.
     This server does not have a graphical user interface.

     @param hostAddress the host address to bind the server to
     @param port        the port number to bind the server to
     */
    public NoGuiServer(String hostAddress, String port) {

        super(new InetSocketAddress(hostAddress, Integer.parseInt(port)));

        this.mapper = new ObjectMapper();
        this.props = new Properties();

        this.setDatabaseSettings();

        this.socketVersion();

        this.initDatabase();
        this.run();
    }

    /**
     * Retrieves the version of the socket.
     * <p>
     * This method reads the version of the socket from the "version.properties" file, and logs the version information.
     * </p>
     * <p>
     * The file "version.properties" should be located in the classpath.
     * </p>
     *
     * @throws RuntimeException if an error occurs while reading the version from the file
     */
    private void socketVersion() {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("version.properties");

        if (inputStream != null) {

            try {

                props.load(inputStream);
                logger.info("Socket version: " + props.getProperty("version"));

            } catch (IOException e) {

                throw new RuntimeException(e);
            }

        }
    }

    /**
     Sets the database settings by retrieving them from environment variables and setting the corresponding properties.

     The method throws a RuntimeException if any of the required environment variables are not set.

     The following environment variables are expected:
     - DB_PATH: the path to the database
     - DB_USER: the username for the database connection
     - DB_PASSWORD: the password for the database connection
     - DB_SSL: whether SSL should be enabled for the database connection

     Once the environment variables are retrieved, they are assigned to the corresponding properties of the object.
     If any of the required environment variables are not set, a RuntimeException is thrown.
     */
    private void setDatabaseSettings() {

        //throw exception if env variables are not set

        //db_path
        Optional<String> db_path = Optional.ofNullable(System.getenv("DB_PATH"));
        this.dbPath = db_path.orElseThrow(() -> new RuntimeException("DB_PATH not set"));

        //db_user
        Optional<String> db_user = Optional.ofNullable(System.getenv("DB_USER"));
        this.props.setProperty("user", db_user.orElseThrow(() -> new RuntimeException("DB_USER not set")));

        //db_password
        Optional<String> db_password = Optional.ofNullable(System.getenv("DB_PASSWORD"));
        this.props.setProperty("password", db_password.orElseThrow(() -> new RuntimeException("DB_PASSWORD not set")));

        //db_ssl - hardcoded for now, since it is not used yet
        this.props.setProperty("ssl", "false");
        //Optional<String> db_ssl = Optional.ofNullable(System.getenv("DB_SSL"));
        //this.props.setProperty("ssl", db_ssl.orElseThrow(() -> new RuntimeException("DB_SSL not set")));
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

                try (Connection connection = DriverManager.getConnection(dbPath, props); Statement statement = connection.createStatement()) {

                    statement.executeUpdate(sqlQuery);
                    logger.info("table created successfully - databasetabel: " + databaseName);

                }

            } catch (SQLException e) {

                logger.log(Level.SEVERE, "Error initializing database", e);
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

        try (Connection connection = DriverManager.getConnection(dbPath, props); ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, null)) {

            return resultSet.next();

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error checking if table exists", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     Called when a Ping message is received from a WebSocket connection.

     @param webSocket The WebSocket connection object representing the connection from which the Ping message was received.
     @param framedata    The Ping message frame data received from the client.
     */
    @Override
    public void onWebsocketPing(WebSocket webSocket, Framedata framedata) {

        super.onWebsocketPing(webSocket, framedata);
    }

    /**
     Called when a Pong frame is received from a WebSocket connection.

     @param webSocket The WebSocket connection object representing the connection from which the Pong frame was received.
     @param framedata    The Pong frame received from the client.
     */
    @Override
    public void onWebsocketPong(WebSocket webSocket, Framedata framedata) {

        if (webSocket.getAttachment() == null) webSocket.setAttachment(new String(framedata.getPayloadData().array()));
    }

    /**
     Called when the WebSocket connection is closing.

     @param webSocket   The WebSocket connection object representing the connection being closed.
     @param code   The status code indicating the reason for closure.
     @param reason The reason for closure provided by the client or the server.
     @param remote Indicates whether the closure was initiated by the remote endpoint (client) or the local endpoint (server).
     */
    @Override
    public void onWebsocketClosing(WebSocket webSocket, int code, String reason, boolean remote) {

        super.onWebsocketClosing(webSocket, code, reason, remote);
    }

    /**
     Called when the WebSocket connection is closing.

     @param webSocket   The WebSocket connection object representing the connection being closed.
     @param code   The status code indicating the reason for closure.
     @param reason The reason for closure provided by the client or the server.
     @param remote Indicates whether the closure was initiated by the remote endpoint (client) or the local endpoint (server).
     */
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason, boolean remote) {

        super.onClosing(webSocket, code, reason, remote);
    }

    /**
     Called when a new WebSocket connection is opened.

     @param webSocket      The WebSocket connection object representing the connection that was opened.
     @param clientHandshake The ClientHandshake object representing the handshake information from the client.
     */
    @Override
    public synchronized void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

        logger.info("User " + webSocket.getRemoteSocketAddress() + " just connected!");

        getAllFromDatabase(webSocket);
        sendMessage(webSocket, "welcome to the server!");
    }

    /**
     Retrieves the data from the database and sends it to the given WebSocket connection.

     @param webSocket The WebSocket connection object representing the connection.
     */
    private synchronized void getAllFromDatabase(WebSocket webSocket) {

//        final String SELECT_SQL = "SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id DESC LIMIT 100;";
        final String SELECT_SQL = "SELECT * FROM (SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id DESC LIMIT 100) AS tmp ORDER BY tmp.id ASC;";

        // actual query
        try (Connection connection = DriverManager.getConnection(dbPath, props); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(SELECT_SQL)) {

            while (resultSet.next()) {

                final String id = fetchID(resultSet);
                final String message = getMessageFromResultSet(resultSet);

                if (id == null) throw new RuntimeException("id is null");
                if (message == null) throw new RuntimeException("message is null");

                final BaseModel baseModel = deserializeMessageModel(message);

                //check if baseModel is instanceof pcitureModel and add image if so
                addImageToPictureModel(baseModel, resultSet);

                setIdIfAvailable(baseModel, id);

                sendMessage(webSocket, mapper.writeValueAsString(baseModel));
            }

            //tell the new client that all messages have been sent
            sendMessage(webSocket, "__startup__end__");

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error retrieving messages from database", e);
            throw new RuntimeException(e.getSQLState());

        } catch (JsonProcessingException e) {

            logger.log(Level.SEVERE, "Error parsing JSON", e);
            throw new RuntimeException(e.getMessage());
        }

    }

    /**
     Sends a message to the specified WebSocket connection.

     @param webSocket The WebSocket connection object representing the connection.
     @param message       The message to be sent.
     */
    private synchronized void sendMessage(final WebSocket webSocket, final String message) {

        webSocket.send(message);
    }

    /**
     Retrieves the ID value from the ResultSet object.

     @param resultSet The ResultSet object containing the data.

     @return The ID value retrieved from the ResultSet.

     @throws SQLException If an error occurs during database access.
     */
    private String fetchID(final ResultSet resultSet) throws SQLException {

        return resultSet.getString("id");
    }

    /**
     Retrieves the "message" field value from the given ResultSet object.

     @param resultSet The ResultSet object from which to retrieve the message value.

     @return The message value as a string.

     @throws SQLException if a database access error occurs.
     */
    private String getMessageFromResultSet(final ResultSet resultSet) throws SQLException {

        return resultSet.getString("message");
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
     @param resultSet           The ResultSet object containing the image data.

     @throws SQLException if there is an error retrieving the image data from the ResultSet.
     */
    private void addImageToPictureModel(final BaseModel messageModel, final ResultSet resultSet) throws SQLException {

        if (messageModel instanceof PictureModel pictureModel) {

            byte[] imageBytes = resultSet.getBytes("image_data");
            pictureModel.setPicture(imageBytes);
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

     @param webSocket   The WebSocket connection object representing the connection.
     @param code   The exit code indicating the reason for the closure.
     @param reason The additional information about the closure.
     @param remote Indicates whether the closure was initiated by the remote endpoint.
     */
    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {

        logger.info("closed " + webSocket.getRemoteSocketAddress() + " " + webSocket.getAttachment() + " with exit code " + code + " additional info: " + reason);
    }

    /**
     Called when a message is received from a WebSocket connection.

     @param webSocket    The WebSocket connection object representing the connection from which the message was received.
     @param message The message received from the client as a string.
     */
    @Override
    public synchronized void onMessage(WebSocket webSocket, String message) {

        final BaseModel baseModel;

        try {

            baseModel = deserializeMessageModel(message);

        } catch (JsonProcessingException e) {

            throw new RuntimeException(e);
        }

        if (baseModel instanceof MessageModel messageModel) {

            switch (messageModel.getMessageType()) {

                case MessageTypes.DELETED, MessageTypes.INTERACTED, MessageTypes.EDITED -> replaceInDatabase(messageModel.getId(), message);

                default -> {

                    saveToDatabase(message);
                    getLastFromDatabase();

                }
            }

        } else if (baseModel instanceof PictureModel) {

            saveImageToDatabase(message);
            getLastFromDatabase();

        }
    }

    /**
     Replaces a message in the database with the given ID and message.

     @param id      The ID of the message to replace.
     @param message The new message to replace the existing message with.
     */
    private synchronized void replaceInDatabase(Long id, String message) {

        // Prepare the UPDATE query
        final String UPDATE_SQL = "UPDATE messages SET message=? WHERE id=?";

        if (dbPath == null) throw new RuntimeException("dbPath is null");

        try {

            try (Connection connection = DriverManager.getConnection(dbPath, props); PreparedStatement updateStatement = connection.prepareStatement(UPDATE_SQL);) {

                //set the parameters
                updateStatement.setString(1, message);
                updateStatement.setLong(2, id);

                // execute -> goal => update one line only!
                if (updateStatement.executeUpdate() == 1) {

                    // If one row was updated, retrieve the updated message from the database
                    PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM messages WHERE id=?");

                    //set the parameters
                    selectStatement.setLong(1, id);

                    //execute the query
                    ResultSet resultSet = selectStatement.executeQuery();

                    //broadcast the message if it exists
                    if (resultSet.next()) broadcast(message);

                } else {

                    // If no rows or more than one were updated, print to stderr
                    System.err.println("Failed to update message with ID " + id);
                }
            }

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error updating message in database", e);
        }
    }

    /**
     Saves the given message to the database.

     @param message the message to be saved
     */
    private synchronized void saveToDatabase(String message) {

        final String INSERT_SQL = "INSERT INTO messages (message) VALUES (?)";

        if (dbPath == null) throw new RuntimeException("dbPath is null");

        try {

            try (Connection connection = DriverManager.getConnection(dbPath, props); PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {

                //set the parameters
                preparedStatement.setString(1, message);

                //execute the query
                preparedStatement.executeUpdate();
            }

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error saving message to database", e);
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

        final String SELECT_SQL = "SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id DESC LIMIT 1;";

        if (dbPath == null) throw new RuntimeException("dbPath is null");

        try {

            try (Connection connection = DriverManager.getConnection(dbPath, props); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(SELECT_SQL)) {

                while (resultSet.next()) {

                    //fetch id and message
                    final String id = fetchID(resultSet);
                    final String message = getMessageFromResultSet(resultSet);

                    //deserialize message
                    BaseModel messageModel = deserializeMessageModel(message);

                    //check if baseModel is instanceof pictureModel and add image if so
                    addImageToPictureModel(messageModel, resultSet);

                    //set id if available
                    messageModel.setId(Long.parseLong(id));

                    //broadcast message
                    broadcast(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageModel));
                }
            }

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error retrieving last message from database", e);
            throw new RuntimeException(e.getSQLState());

        } catch (JsonProcessingException e) {

            logger.log(Level.SEVERE, "Error parsing JSON", e);
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

        //save the message (message table is linked to the picture table) to the database and get id
        messageId = saveMessage(updatedPictureModelJson);

        //save the image to the database
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

            logger.log(Level.SEVERE, "Error parsing JSON", e);
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

            logger.log(Level.SEVERE, "Error parsing JSON", e);
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

        final String INSERT_MESSAGE_SQL = "INSERT INTO messages (message) VALUES (?) RETURNING id";

        try (
                //create the connection and the prepared statement
                Connection connection = DriverManager.getConnection(dbPath, props); PreparedStatement preparedStatement = connection.prepareStatement(INSERT_MESSAGE_SQL)) {

            //set the parameters
            preparedStatement.setString(1, updatedPictureModelJson);

            //execute the query and return the generated id
            return saveToDataBaseAndReturnId(preparedStatement);

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error saving message to database", e);
            throw new RuntimeException(e);
        }
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
        final String INSERT_IMAGE_SQL = "INSERT INTO message_images (message_id, image_data) VALUES (?, ?)";

        try (
                //create the connection and the prepared statement
                Connection connection = DriverManager.getConnection(dbPath, props); PreparedStatement preparedStatement = connection.prepareStatement(INSERT_IMAGE_SQL)) {

            //save the image to the database
            saveImageToDatabase(preparedStatement, messageId, imageBytes);

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error saving image to database", e);
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

     @param preparedStatement the PreparedStatement object for executing the SQL query

     @return the generated ID for the saved message

     @throws SQLException if there is an error executing the SQL query or fetching the generated ID
     */
    private long saveToDataBaseAndReturnId(final PreparedStatement preparedStatement) throws SQLException {

        // fetch generated ID
        try (ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {

                return resultSet.getLong(1);

            } else {

                logger.log(Level.SEVERE, "Error saving message to database");
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

     @param preparedStatement the prepared statement used to execute the update statement
     @param messageId         the ID of the message to which the image belongs
     @param imageBytes        the byte array of the image to be saved

     @throws SQLException if there is an error executing the update statement
     */
    private void saveImageToDatabase(final PreparedStatement preparedStatement, final long messageId, final byte[] imageBytes) throws SQLException {

        //set the parameters
        preparedStatement.setLong(1, messageId);
        preparedStatement.setBytes(2, imageBytes);

        //execute the query
        preparedStatement.executeUpdate();
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

        logger.log(Level.SEVERE, "Error on connection " + conn.getRemoteSocketAddress() + ":" + ex);
        logger.log(Level.SEVERE, "server might be running already!", ex);
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

        logger.info("***");
        logger.info("server started successfully with ip " + this.getAddress().getHostString() + " and port " + this.getAddress().getPort() + "!");
        logger.info("***");
    }

    /**
     Callback method invoked when a message is received by the WebSocket server.

     This method broadcasts the received message to all connected clients.

     @param webSocket  The WebSocket connection object.
     @param byteBuffer The message received, as a ByteBuffer.
     */
    @Override
    public synchronized void onMessage(WebSocket webSocket, ByteBuffer byteBuffer) {

        broadcast(byteBuffer);
    }
}