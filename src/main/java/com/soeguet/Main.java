package com.soeguet;

import com.soeguet.controller.MessagesController;
import com.soeguet.controller.interfaces.MessagesControllerInterface;
import com.soeguet.database.DatabaseConnection;
import com.soeguet.database.interfaces.DatabaseConnectionController;
import com.soeguet.nogui.NoGuiServer;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

/**
 Represents the main class of the application.
 This class is responsible for parsing command line arguments, initializing the argumentsMap,
 and creating a new instance of the NoGuiServer class.
 */
public class Main {

    final Map<String, String> argumentsMap;
    private final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     Initializes a new instance of the Main class.
     This constructor parses the command line arguments, initializes the argumentsMap,
     and creates a new NoGuiServer instance with the parsed "ip" and "port" arguments.

     @param args The command line arguments.
     */
    public Main(String... args) {

        //parse command line arguments
        argumentsMap = parseCommandLineArguments(args);

        //create a property object and fill it with the parsed arguments
        final Properties properties = initializeProperties();

        //print socket version to console 
        this.socketVersion(properties);

        //start actual socket server
        final WebSocketServer webSocketServer = initializeWebSocketServer(properties);
        webSocketServer.run();
    }

    /**
     * Initializes the Properties object and sets the "ip" and "port" properties based on the parsed command line arguments.
     *
     * @return The initialized Properties object.
     */
    private Properties initializeProperties() {

        Properties properties = new Properties();

        properties.setProperty("ip", parsedArgument("ip"));
        properties.setProperty("port", parsedArgument("port"));

        return properties;
    }

    /**
     Initializes and returns a new instance of the WebSocketServer class.
     This method creates a new DatabaseConnectionController instance with the provided properties,
     initializes the database settings, and initializes the database.
     Then, it creates a new MessagesController instance with the database connection controller.
     Finally, it creates a new NoGuiServer instance with the message controller and the properties.

     @param properties The properties object containing the "ip" and "port" settings.
     @return A new instance of the WebSocketServer class.
     */
    private WebSocketServer initializeWebSocketServer(final Properties properties) {

        //dependency for message controller - handles all database interactions
        DatabaseConnectionController databaseConnectionController = new DatabaseConnection(properties);
        databaseConnectionController.setDatabaseSettings();
        databaseConnectionController.initDatabase();

        //dependency for socket server - handles all messages
        MessagesControllerInterface messagesControllerInterface = new MessagesController(databaseConnectionController);

        return new NoGuiServer(messagesControllerInterface, properties);
    }

    /**
     Parses the command line arguments and returns a map of the parsed arguments.
     The arguments should be in the form of key-value pairs separated by an equals sign (=).
     Only arguments in the correct format will be included in the resulting map.

     @param args the command line arguments to be parsed

     @return a map containing the parsed arguments
     */
    private Map<String, String> parseCommandLineArguments(final String... args) {

        Map<String, String> arguments = new HashMap<>();

        for (String arg : args) {
            String[] split = arg.split("=");
            if (split.length == 2) {
                arguments.put(split[0], split[1]);
            }
        }
        return arguments;
    }

    /**
     Retrieves the version of the socket.
     <p>
     This method reads the version of the socket from the "version.properties" file, and logs the version information.
     </p>
     <p>
     The file "version.properties" should be located in the classpath.
     </p>

     @throws RuntimeException if an error occurs while reading the version from the file
     */
    private void socketVersion(final Properties properties) {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("version.properties");

        if (inputStream != null) {

            try {

                properties.load(inputStream);
                logger.info("Socket version: " + properties.getProperty("version"));

            } catch (IOException e) {

                throw new RuntimeException(e);
            }

        }
    }

    /**
     The entry point of the application.

     @param args the command line arguments
     */
    public static void main(String... args) {

        new Main(args);
    }

    /**
     Retrieves the value of the specified argument from the argument's map.
     If the argument is found, its value is returned.
     If it is not found, an IllegalArgumentException is thrown.

     @param argument the argument whose value is to be retrieved

     @return the value of the specified argument

     @throws IllegalArgumentException if the argument is missing
     */
    private String parsedArgument(final String argument) {

        return Optional.ofNullable(argumentsMap.get(argument)).orElseThrow(() -> new IllegalArgumentException("Argument " + argument + " is missing"));
    }
}