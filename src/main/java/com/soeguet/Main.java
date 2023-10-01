package com.soeguet;

import com.soeguet.nogui.NoGuiServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the main class of the application.
 * This class is responsible for parsing command line arguments, initializing the argumentsMap,
 * and creating a new instance of the NoGuiServer class.
 */
public class Main {

    final Map<String, String> argumentsMap;

    /**
     * Initializes a new instance of the Main class.
     * This constructor parses the command line arguments, initializes the argumentsMap,
     * and creates a new NoGuiServer instance with the parsed "ip" and "port" arguments.
     *
     * @param args The command line arguments.
     */
    public Main(String... args) {

        argumentsMap = parseCommandLineArguments(args);
        new NoGuiServer(parsedArgument("ip"), parsedArgument("port"));
    }

    /**
     * Parses the command line arguments and returns a map of the parsed arguments.
     * The arguments should be in the form of key-value pairs separated by an equals sign (=).
     * Only arguments in the correct format will be included in the resulting map.
     *
     * @param args the command line arguments to be parsed
     * @return a map containing the parsed arguments
     */
    private Map<String, String> parseCommandLineArguments(final String[] args) {

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
     * Retrieves the value of the specified argument from the arguments map.
     * If the argument is found, its value is returned. If it is not found, an IllegalArgumentException is thrown.
     *
     * @param argument the argument whose value is to be retrieved
     * @return the value of the specified argument
     * @throws IllegalArgumentException if the argument is missing
     */
    private String parsedArgument(final String argument) {

        return Optional.ofNullable(argumentsMap.get(argument)).orElseThrow(() -> new IllegalArgumentException("Argument " + argument + " is missing"));
    }

    /**
     * The entry point of the application.
     *
     * @param args the command line arguments
     */
    public static void main(String... args) {

        new Main(args);
    }
}