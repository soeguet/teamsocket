package com.soeguet.nogui;

import com.soeguet.controller.interfaces.MessagesControllerInterface;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NoGuiServer extends WebSocketServer {

    private final Logger logger = Logger.getLogger(NoGuiServer.class.getName());

    private final MessagesControllerInterface messageController;

    public NoGuiServer(final MessagesControllerInterface messageController, final Properties properties) {

        super(new InetSocketAddress(properties.getProperty("ip"), Integer.parseInt(properties.getProperty("port"))));
        this.messageController = messageController;

    }

    /**
     Called when a Ping message is received from a WebSocket connection.

     @param webSocket The WebSocket connection object representing the connection from which the Ping message was received.
     @param framedata The Ping message frame data received from the client.
     */
    @Override
    public void onWebsocketPing(WebSocket webSocket, Framedata framedata) {

        super.onWebsocketPing(webSocket, framedata);
    }

    @Override
    public void onWebsocketPong(WebSocket webSocket, Framedata framedata) {

        //TODO
        if (webSocket.getAttachment() == null) webSocket.setAttachment(new String(framedata.getPayloadData().array()));
    }

    /**
     Called when the WebSocket connection is closing.

     @param webSocket The WebSocket connection object representing the connection being closed.
     @param code      The status code indicating the reason for closure.
     @param reason    The reason for closure provided by the client or the server.
     @param remote    Indicates whether the closure was initiated by the remote endpoint (client) or the local endpoint (server).
     */
    @Override
    public void onWebsocketClosing(WebSocket webSocket, int code, String reason, boolean remote) {

        super.onWebsocketClosing(webSocket, code, reason, remote);
    }

    /**
     Called when the WebSocket connection is closing.

     @param webSocket The WebSocket connection object representing the connection being closed.
     @param code      The status code indicating the reason for closure.
     @param reason    The reason for closure provided by the client or the server.
     @param remote    Indicates whether the closure was initiated by the remote endpoint (client) or the local endpoint (server).
     */
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason, boolean remote) {

        super.onClosing(webSocket, code, reason, remote);
    }

    /**
     Called when the WebSocket connection is opened. Sends 100 last messages to the freshly connected client.

     @param webSocket         The WebSocket connection object representing the connection that has been opened.
     @param clientHandshake   The handshake information provided by the client.
     */
    @Override
    public synchronized void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

        logger.info("+++ User " + webSocket.getRemoteSocketAddress() + " just connected! +++");

        final Deque<String> messageHistory = messageController.retrieveLast100Messages();

        if (messageHistory != null) {

            messageHistory.forEach(webSocket::send);
        }
    }

    /**
     Called when the WebSocket connection is closed.

     @param webSocket The WebSocket connection object representing the connection.
     @param code      The exit code indicating the reason for the closure.
     @param reason    The additional information about the closure.
     @param remote    Indicates whether the closure was initiated by the remote endpoint.
     */
    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {

        logger.info("closed " + webSocket.getRemoteSocketAddress() + " " + webSocket.getAttachment() + " with exit code " + code + " additional info: " + reason);
    }

    @Override
    public synchronized void onMessage(WebSocket webSocket, String message) {

        final String broadcastMessage = messageController.onMessageFromClient(message);

        broadcast(broadcastMessage);
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
     including the IP address and port on which it is listening to.

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

        //TODO javadoc
        broadcast(byteBuffer);
    }
}