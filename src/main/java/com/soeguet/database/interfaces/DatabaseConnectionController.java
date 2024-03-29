package com.soeguet.database.interfaces;

import com.soeguet.model.dtos.DatabaseResult;

import java.util.Deque;
import java.util.Optional;

/**
 * The DatabaseConnectionController interface provides methods to interact with a database.
 */
public interface DatabaseConnectionController {

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName the name of the table to check
     * @return true if the table exists, false otherwise
     */
    boolean checkIfTableExists(String tableName);

    /**
     * Initializes the database by creating the necessary tables if they don't exist.
     */
    void initDatabase();

    /**
     * Retrieves the value of the specified environment variable.
     *
     * @param variableName The name of the environment variable.
     * @return An {@link Optional} containing the value of the environment variable if it exists,
     *         or an empty {@link Optional} if the variable does not exist.
     */
    Optional<String> retrieveEnvironmentVariables(final String variableName);

    /**
     * Sets the database settings for connecting to the database.
     * This method should be called before initializing the database.
     * The database settings include the database URL, timeAndUsername, and password.
     */
    void setDatabaseSettings();

    /**
     * Retrieves all data from the database.
     * This method executes a query to retrieve all records from the database table.
     *
     * @return A Deque of DatabaseResult objects that represents the result set of the query.
     * The result set contains the retrieved data from the database.
     * The result set is returned as a Deque, which is a double-ended queue,
     * allowing for efficient insertion and removal of elements from both ends of the queue.
     */
    Deque<DatabaseResult> getAllFromDatabase();

    /**
     * Replaces a specific record in the database with a new message.
     * This method updates the message column of a specific record in the database table with the given id.
     * If a record with the specified id does not exist in the table, no operation will be performed.
     *
     * @param id      The unique identifier of the record to be replaced.
     * @param message The new message to replace the existing message in the record.
     *                This should be a non-null string.
     */
    void replaceInDatabase(Long id, String message);

    /**
     * Saves the given message to the database.
     * This method inserts a new record into the database table with the given message.
     *
     * @param message The message to be saved in the database.
     *                This should be a non-null string.
     */
    void saveToDatabase(String message);

    /**
     * Retrieves the last record from the database.
     * This method retrieves the latest record from the database table and returns it as a DatabaseResult object.
     *
     * @return A DatabaseResult object containing the last record from the database.
     * The DatabaseResult object will have the following columns: [column1, column2, ...]
     * If no records are found, an empty DatabaseResult object will be returned.
     */
    DatabaseResult getLastFromDatabase();

    /**
     * Saves a message to the database.
     * This method takes a JSON representation of an updated picture model and saves it as a message in the database.
     * The method returns the generated ID of the saved message as a long value.
     *
     * @param updatedPictureModelJson A String containing the JSON representation of the updated picture model.
     *                                The JSON format should follow the specified schema: { "id": 1, "name": "example" }
     *                                The "id" field is optional and will be generated by the database if not provided.
     *                                The "name" field is required and should be a non-empty string.
     * @return The generated ID of the saved message as a long value.
     *         Returns -1 if there was an error saving the message to the database.
     */
    long saveMessageWithoutPictureToDatabase(final String updatedPictureModelJson);

    /**
     * Saves an image associated with a message.
     * This method takes the ID of a message and the byte array representing an image
     * and saves the image in the database associated with the given message.
     *
     * @param messageId The ID of the message to which the image belongs.
     *                  This ID should correspond to a valid message in the database.
     * @param imageBytes The byte array representing the image to be saved.
     *                   The image should be in a supported format such as JPEG or PNG.
     *                   The byte array should not be null or empty.
     * @throws IllegalArgumentException If the messageId is less than or equal to 0,
     *                                  or if the imageBytes parameter is null or empty.
     */
    void saveExtractedImageToDatabaseInImageTable(final long messageId, final byte[] imageBytes);

    /**
     * Retrieves an updated entry from the database.
     * This method takes the ID of an updated entry and returns the corresponding entry as a string.
     *
     * @param updatedId The ID of the updated entry to be retrieved from the database.
     *                  This ID should correspond to a valid entry in the database.
     * @return The updated entry as a string.
     * @throws IllegalArgumentException If the updatedId is null or less than or equal to 0.
     */
    String retrieveUpdatedEntry(Long updatedId);

    String getMessageFromDatabase(Long aLong);
}