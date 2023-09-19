package com.soeguet.nogui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.soeguet.model.jackson.MessageModel;
import com.soeguet.util.MessageTypes;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Arrays;
import java.util.Properties;

public class NoGuiServer extends WebSocketServer {

    private final String dbPath;

    private final Properties props;

    private final ObjectMapper mapper;

    private java.util.logging.Logger log = java.util.logging.Logger.getLogger(NoGuiServer.class.getName());

    public NoGuiServer(String hostAddress, int port) {

        super(new InetSocketAddress(hostAddress, port));

        mapper = new ObjectMapper();

        dbPath = "jdbc:postgresql://localhost:5432/postgres";
        props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "postgres");
        props.setProperty("ssl", "false");

        initDatabase();

        run();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

        log.info("User " + conn.getRemoteSocketAddress() + " just connected!");
        getAllFromDatabase(conn);
        conn.send("welcome to the server!");
        conn.sendPing();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        System.out.println(message);

        try {

            MessageModel messageModel = mapper.readValue(message, MessageModel.class);

            if (messageModel.getMessageType() == MessageTypes.DELETED || messageModel.getMessageType() == MessageTypes.INTERACTED || messageModel.getMessageType() == MessageTypes.EDITED) {
                replaceInDatabase(messageModel.getId(), message);
            } else {
                saveToDatabase(message);
                getLastFromDatabase();

                // remove is typing.. for all clients
                broadcast("X".getBytes());
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onWebsocketPing(WebSocket conn, Framedata f) {

        super.onWebsocketPing(conn, f);
        broadcast("X".getBytes());
    }

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

    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {

        if (conn.getAttachment() == null) conn.setAttachment(new String(f.getPayloadData().array()));
    }

    private void initDatabase() {

        String createTableSql = "CREATE TABLE IF NOT EXISTS messages (id BIGSERIAL PRIMARY KEY, message VARCHAR(2255) NOT NULL);";

        try {
            assert dbPath != null;
            try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createTableSql);
                log.info("table created successfully");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveToDatabase(String message) {

        String insertSql = "INSERT INTO messages (message) VALUES (?)";

        try {
            assert dbPath != null;
            try (Connection conn = DriverManager.getConnection(dbPath, props); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, message);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
            throw new RuntimeException(e);
        }
    }

    private void getAllFromDatabase(WebSocket connWebsocket) {

        String selectSql = "SELECT * FROM (SELECT * FROM messages ORDER BY id DESC LIMIT 100) subquery ORDER BY id;";

        // get max row count
        try {
            assert dbPath != null;
            try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); ResultSet rs = stmt.executeQuery(selectSql)) {

                // rs.last();
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

                MessageModel messageModel = mapper.readValue(message, MessageModel.class);

                if (messageModel.getId() == null) messageModel.setId(Long.parseLong(id));
                connWebsocket.send(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageModel));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void getLastFromDatabase() {

        String selectSql = "SELECT * FROM (SELECT * FROM messages ORDER BY id DESC LIMIT 1) AS last_entry";

        try {
            assert dbPath != null;
            try (Connection conn = DriverManager.getConnection(dbPath, props); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(selectSql)) {

                while (rs.next()) {

                    String id = rs.getString("id");
                    String message = rs.getString("message");

                    MessageModel messageModel = mapper.readValue(message, MessageModel.class);
                    messageModel.setId(Long.parseLong(id));

                    broadcast(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageModel));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        log.info("closed " + conn.getRemoteSocketAddress() + " " + conn.getAttachment() + " with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        log.info("an error occurred on connection " + conn.getRemoteSocketAddress() + ":" + ex);
        log.info("server might be running already!");
        log.info("ex.getStackTrace() = " + Arrays.toString(ex.getStackTrace()));
    }

    @Override
    public void onStart() {

        log.info("server started successfully");
    }

    @Override
    public void onWebsocketClosing(WebSocket conn, int code, String reason, boolean remote) {

        super.onWebsocketClosing(conn, code, reason, remote);
    }

    @Override
    public void onClosing(WebSocket conn, int code, String reason, boolean remote) {

        super.onClosing(conn, code, reason, remote);
    }
}