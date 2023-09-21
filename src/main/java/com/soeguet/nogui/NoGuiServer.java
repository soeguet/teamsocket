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
import java.util.concurrent.Executors;
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

        initiateDatabase(createTableSql);
        initiateDatabase(createTableSqlImages);
    }

    /**
     Initializes the database by creating the necessary table if it does not already exist.

     @param sqlQuery the SQL statement to create the table
     */
    private void initiateDatabase(String sqlQuery) {

        try {

            assert dbPath != null;

            try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement()) {

                stmt.executeUpdate(sqlQuery);
                LOGGER.info("table created successfully");

            }

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error initializing database", e);
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
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

        LOGGER.info("User " + conn.getRemoteSocketAddress() + " just connected!");
        getAllFromDatabase(conn);
        conn.send("welcome to the server!");
        conn.sendPing();
    }

    /**
     Retrieves the data from the database and sends it to the given WebSocket connection.

     @param connWebsocket The WebSocket connection object representing the connection.
     */
    private void getAllFromDatabase(WebSocket connWebsocket) {

        String selectSql = "SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id LIMIT 100;";

        // get max row count
        try {

            assert dbPath != null;

            try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); ResultSet rs = stmt.executeQuery(selectSql)) {

                int row = rs.getFetchSize();
                connWebsocket.send("ROWS:" + row);

            }

        } catch (SQLException e) {

            throw new RuntimeException(e);
        }

        // actual query
        try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {

                String id = rs.getString("id");
                String message = rs.getString("message");

                BaseModel messageModel = mapper.readValue(message, BaseModel.class);

                if (messageModel instanceof PictureModel) {

                    byte[] imageBytes = rs.getBytes("image_data");
                    ((PictureModel) messageModel).setPicture(imageBytes);
                }

                if (messageModel.getId() == null) {

                    messageModel.setId(Long.parseLong(id));
                }

                connWebsocket.send(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageModel));

            }

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error retrieving messages from database", e);

        } catch (JsonProcessingException e) {

            LOGGER.log(Level.SEVERE, "Error parsing JSON", e);
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
    public void onMessage(WebSocket conn, String message) {

        try {

            BaseModel baseModel = mapper.readValue(message, BaseModel.class);

            if (baseModel instanceof MessageModel) {

                switch (((MessageModel) baseModel).getMessageType()) {
                    case MessageTypes.DELETED, MessageTypes.INTERACTED, MessageTypes.EDITED -> replaceInDatabase(baseModel.getId(), message);
                    default -> {

                        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                            executor.submit(() -> {

                                saveToDatabase(message);
                                getLastFromDatabase();
                            });
                        }
                    }
                }
            } else if (baseModel instanceof PictureModel) {
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                    executor.submit(() -> {

                        saveImageToDatabase(message);
                        getLastFromDatabase();
                    });
                }
            }

        } catch (JsonProcessingException e) {

            LOGGER.log(Level.SEVERE, "Error parsing JSON", e);
        }

        // remove is typing.. for all clients
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            executor.submit(() -> {

                broadcast("X".getBytes());
            });
        }
    }

    /**
     Replaces a message in the database with the given ID and message.

     @param id      The ID of the message to replace.
     @param message The new message to replace the existing message with.
     */
    private void replaceInDatabase(Long id, String message) {

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
    private void saveToDatabase(String message) {

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

     This method retrieves the last message from the `messages` table in the database. It uses a SQL query to select the last entry based on the `id` column. The retrieved message is then mapped to a `MessageModel` object using JSON deserialization. The `id` field of the `MessageModel` is set based on the retrieved `id` from the database. Finally, the broadcast method is called to broadcast the serialized `MessageModel` as a JSON string.
     */
    private void getLastFromDatabase() {

//        String selectSql = "SELECT * FROM (SELECT * FROM messages ORDER BY id DESC LIMIT 1) AS last_entry";
        String selectSql = "SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id DESC LIMIT 1;";

        try {

            assert dbPath != null;

            try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(selectSql)) {

                while (rs.next()) {

                    String id = rs.getString("id");
                    String message = rs.getString("message");

                    BaseModel messageModel = mapper.readValue(message, BaseModel.class);

                    if (messageModel instanceof PictureModel) {

                        byte[] imageBytes = rs.getBytes("image_data");
                        ((PictureModel) messageModel).setPicture(imageBytes);
                    }

                    messageModel.setId(Long.parseLong(id));

                    broadcast(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageModel));
                }
            }

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE, "Error retrieving last message from database", e);

        } catch (JsonProcessingException e) {

            LOGGER.log(Level.SEVERE, "Error parsing JSON", e);
        }
    }

    private void saveImageToDatabase(String message) {

        assert dbPath != null;

        PictureModel pictureModel = null;
        try {
            pictureModel = mapper.readValue(message, PictureModel.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        byte[] imageBytes = pictureModel.getPicture();
        pictureModel.setPicture(null);

        long messageId;

        String updatedPictureModelJson = null;
        try {
            updatedPictureModelJson = mapper.writeValueAsString(pictureModel);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String insertMessageSql = "INSERT INTO messages (message) VALUES (?) RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbPath, props); PreparedStatement pstmt = conn.prepareStatement(insertMessageSql)) {

            pstmt.setString(1, updatedPictureModelJson);

            // fetch generated ID
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    messageId = rs.getLong(1);
                } else {
                    throw new SQLException("no ID generated");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // insert image into database
        String insertImageSql = "INSERT INTO message_images (message_id, image_data) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbPath, props); PreparedStatement pstmt = conn.prepareStatement(insertImageSql)) {
            pstmt.setLong(1, messageId);
            pstmt.setBytes(2, imageBytes);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        System.out.println("image saved to database");

    }

    /**
     Handles an error that occurs on a WebSocket connection.

     This method logs the error message along with the remote socket address of the connection.

     @param conn the WebSocket connection on which the error occurred
     @param ex   the exception that represents the error
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {

        LOGGER.log(Level.SEVERE, "Error on connection " + conn.getRemoteSocketAddress() + ":" + ex);
        LOGGER.log(Level.SEVERE, "server might be running already!", ex);
    }

    /**
     Callback method invoked when the WebSocket server has started successfully.

     This method logs a message indicating that the server has started successfully.
     */
    @Override
    public void onStart() {

        LOGGER.info("server started successfully with ip " + this.getAddress().getHostString() + " and port " + this.getAddress().getPort() + "!");
    }

    /**
     Called when a message is received from a WebSocket connection.

     @param conn    The WebSocket connection object representing the connection from which the message was received.
     @param message The message received from the client as a ByteBuffer.
     */
    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {

        // send back list of participants
        if (new String(message.array()).equals("PARTICIPANTLIST")) {

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("PARTICIPANTS:");
            super.getConnections().forEach(a -> stringBuilder.append(a).append(","));

            broadcast(String.valueOf(stringBuilder).getBytes());
            return;
        }

        // close all clients
        if (new String(message.array()).equals("/terminateAll")) {
            broadcast(message.array());
            return;
        }

        // broadcast pictures
        if (message.array().length > 50) {

            broadcast(message);
            return;
        }

        broadcast(((String) conn.getAttachment()).getBytes());
    }
}