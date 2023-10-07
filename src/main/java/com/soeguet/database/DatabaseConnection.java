package com.soeguet.database;

import com.soeguet.database.interfaces.DatabaseConnectionController;
import com.soeguet.model.dtos.DatabaseResult;

import java.sql.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection implements DatabaseConnectionController {

    private final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private final Properties properties;

    private String dbPath;

    public DatabaseConnection(final Properties properties) {

        this.properties = properties;
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
    public void saveExtractedImageToDatabaseInImageTable(final long messageId, final byte[] imageBytes) {

        // insert image into database
        final String INSERT_IMAGE_SQL = "INSERT INTO message_images (message_id, image_data) VALUES (?, ?)";

        try (Connection connection = DriverManager.getConnection(dbPath, properties); PreparedStatement preparedStatement = connection.prepareStatement(INSERT_IMAGE_SQL)) {

            //save the image to the database
            saveImageToDatabase(preparedStatement, messageId, imageBytes);

        } catch (SQLException e) {

            this.logger.log(Level.SEVERE, "Error saving image to database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Retrieves the updated entry for a given ID from the database.

     <p>
     This method takes a Long value representing the ID of the entry to be retrieved. It executes a SQL SELECT
     statement with a WHERE clause to retrieve the updated entry with the specified ID from the "messages" table
     in the database. The result is returned as a String.
     </p>

     @param updatedId the ID of the entry to retrieve

     @return the updated entry as a String

     @throws RuntimeException if there is an error retrieving the entry from the database
     */
    @Override
    public synchronized String retrieveUpdatedEntry(final Long updatedId) {

        final String RETRIEVE_UPDATED_LINE = "SELECT * FROM messages WHERE id=?";

        try (Connection connection = DriverManager.getConnection(this.dbPath, this.properties)) {

            // If one row was updated, retrieve the updated message from the database
            PreparedStatement selectStatement = connection.prepareStatement(RETRIEVE_UPDATED_LINE);

            //set the parameters
            selectStatement.setLong(1, updatedId);

            //execute the query and return
            return selectStatement.executeQuery().getString(2);

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error retrieving updated entry from database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Saves an image associated with a message to the database.

     @param preparedStatement the prepared statement to execute the INSERT query
     @param messageId         the ID of the message to associate the image with
     @param imageBytes        an array of bytes representing the image data

     @throws RuntimeException if there is an error saving the image to the database
     */
    private void saveImageToDatabase(final PreparedStatement preparedStatement, final long messageId, final byte[] imageBytes) {

        try {

            //set the parameters
            preparedStatement.setLong(1, messageId);
            preparedStatement.setBytes(2, imageBytes);

            //execute the query
            preparedStatement.executeUpdate();

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error saving image to database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Sets the database settings by retrieving them from environment variables and setting the corresponding properties.

     The method throws a RuntimeException if any of the required environment variables are not set.

     The following environment variables are expected
     - DB_PATH: the path to the database
     - DB_USER: the username for the database connection
     - DB_PASSWORD: the password for the database connection
     - DB_SSL: whether SSL should be enabled for the database connection

     Once the environment variables are retrieved, they are assigned to the corresponding properties of the object.
     If any of the required environment variables are not set, a RuntimeException is thrown.
     */
    public void setDatabaseSettings() {

        //db_path
        Optional<String> db_path = Optional.ofNullable(System.getenv("DB_PATH"));
        this.dbPath = db_path.orElseThrow(() -> new RuntimeException("DB_PATH not set"));

        //db_user
        Optional<String> db_user = Optional.ofNullable(System.getenv("DB_USER"));
        this.properties.setProperty("user", db_user.orElseThrow(() -> new RuntimeException("DB_USER not set")));

        //db_password
        Optional<String> db_password = Optional.ofNullable(System.getenv("DB_PASSWORD"));
        this.properties.setProperty("password", db_password.orElseThrow(() -> new RuntimeException("DB_PASSWORD not set")));

        //db_ssl - hardcoded for now, since it is not used yet
        this.properties.setProperty("ssl", "false");
    }

    /**
     Initializes the database by creating the necessary table if it does not already exist.
     */
    @Override
    public void initDatabase() {

        String createTableSql = "CREATE TABLE IF NOT EXISTS messages (id BIGSERIAL PRIMARY KEY, message VARCHAR(2255) NOT NULL);";
        String createTableSqlImages = "CREATE TABLE IF NOT EXISTS message_images (id BIGSERIAL PRIMARY KEY, message_id BIGINT REFERENCES messages(id), image_data bytea NOT NULL);";

        initiateDatabaseQuery("messages", createTableSql);
        initiateDatabaseQuery("message_images", createTableSqlImages);
    }

    /**
     Initializes the database by creating the necessary table if it does not already exist.

     @param sqlQuery the SQL statement to create the table
     */
    private void initiateDatabaseQuery(String databaseName, String sqlQuery) {

        if (!checkTableExists(databaseName)) {

            try (Connection connection = DriverManager.getConnection(this.dbPath, this.properties); Statement statement = connection.createStatement()) {

                statement.executeUpdate(sqlQuery);
                this.logger.info("table created successfully - database table: " + databaseName);

            } catch (SQLException e) {

                logger.log(Level.SEVERE, "Error creating table", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     Checks if the specified table exists in the database.

     @param tableName the name of the table to check

     @return true if the table exists, false otherwise
     */
    public boolean checkTableExists(String tableName) {

        try (Connection connection = DriverManager.getConnection(this.dbPath, this.properties); ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, null)) {

            if (resultSet.next()) {

                return resultSet.next();

            } else {

                return false;
            }

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error checking if table exists", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     Replaces a message in the database with the specified ID.

     @param id      the ID of the message to replace
     @param message the new message to replace with

     @throws RuntimeException if an error occurs while replacing the message
     */
    public synchronized void replaceInDatabase(Long id, String message) {

        final String UPDATE_SQL = "UPDATE messages SET message=? WHERE id=?";

        try (Connection connection = DriverManager.getConnection(this.dbPath, this.properties)) {

            //UPDATE query
            PreparedStatement updateStatement = connection.prepareStatement(UPDATE_SQL);

            //set the parameters
            updateStatement.setString(1, message);
            updateStatement.setLong(2, id);

            // execute -> goal => update one line only!
            final int updatedRows = updateStatement.executeUpdate();

            if (updatedRows != 1) {

                throw new SQLWarning("No rows updated");
            }

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error replacing message in database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Saves the given message to the database.

     @param message the message to be saved
     */
    public synchronized void saveToDatabase(String message) {

        final String INSERT_SQL = "INSERT INTO messages (message) VALUES (?)";

        try {

            try (Connection connection = DriverManager.getConnection(dbPath, properties)) {

                PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);

                //set the parameters
                preparedStatement.setString(1, message);

                //execute the query
                final int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected != 1) {

                    throw new SQLWarning("Message was not saved to database!");
                }
            }

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error saving message to database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Retrieves the last message from the database.

     @return a DatabaseResult object containing the id, message, and image data of the last message

     @throws RuntimeException if an error occurs while retrieving the last message
     */
    public synchronized DatabaseResult getLastFromDatabase() {

        final String SELECT_SQL = "SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id DESC LIMIT 1;";

        try (Connection connection = DriverManager.getConnection(dbPath, properties); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(SELECT_SQL)) {

            if (resultSet.next()) {

                return new DatabaseResult(resultSet.getLong(1), resultSet.getString(2), resultSet.getBytes(3));

            } else {

                throw new SQLWarning("No rows found");
            }

        } catch (SQLException e) {

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
    public long saveMessageWithoutPictureToDatabase(final String updatedPictureModelJson) {

        final String INSERT_MESSAGE_SQL = "INSERT INTO messages (message) VALUES (?) RETURNING id";

        try (Connection connection = DriverManager.getConnection(this.dbPath, this.properties)) {

            //set the parameters
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_MESSAGE_SQL);
            preparedStatement.setString(1, updatedPictureModelJson);

            //execute the query and return the generated id
            return saveToDataBaseAndReturnId(preparedStatement);

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error saving message to database", e);
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
     */
    private long saveToDataBaseAndReturnId(final PreparedStatement preparedStatement) {

        // fetch generated ID
        try (ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {

                return resultSet.getLong(1);

            } else {

                logger.log(Level.SEVERE, "Error saving message to database");
                throw new SQLException("no ID generated");
            }

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error saving message to database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     Fetches all messages from the database.

     <p>
     This method retrieves all messages from the database by executing the specified SQL query.
     The messages are fetched along with their IDs and image data (if available).
     The results are stored in a Deque of DatabaseResult objects and returned.
     If there is an error executing the query, a SQLException is thrown.
     </p>

     @return a Deque of DatabaseResult objects representing the retrieved messages

     @throws RuntimeException if there is an error retrieving the messages from the database
     */
    public synchronized Deque<DatabaseResult> getAllFromDatabase() {

        final String SELECT_SQL = "SELECT * FROM (SELECT messages.id, messages.message, message_images.image_data FROM messages LEFT JOIN message_images ON messages.id = message_images.message_id ORDER BY messages.id DESC LIMIT 100) AS tmp ORDER BY tmp.id ASC;";

        Deque<DatabaseResult> messageQueue = new ArrayDeque<>();

        // actual query
        try (Connection connection = DriverManager.getConnection(dbPath, this.properties); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(SELECT_SQL)) {

            while (resultSet.next()) {

                final long id = resultSet.getLong(1);
                final String message = resultSet.getString(2);
                final byte[] image = resultSet.getBytes(3);

                messageQueue.add(new DatabaseResult(id, message, image));
            }

            return messageQueue;

        } catch (SQLException e) {

            logger.log(Level.SEVERE, "Error retrieving all messages from database", e);
            throw new RuntimeException(e.getSQLState());
        }
    }
}