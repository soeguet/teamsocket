package com.soeguet.database;

import jdk.jfr.Name;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseIntegrationTest {

    private static Connection connection;

    @BeforeAll
    public static void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
    }

    @Test
    @DisplayName("Test if database environment variables are set")
    public void testInitDatabase() throws SQLException {

        //properties setup
        Properties properties = new Properties();
        properties.setProperty("user", "sa");
        properties.setProperty("password", "");

        //mock setup
        DatabaseConnection databaseConnection = Mockito.spy(DatabaseConnection.class);
        Mockito.when(databaseConnection.getDbPath()).thenReturn("jdbc:h2:mem:testdb");
        Mockito.when(databaseConnection.getProperties()).thenReturn(properties);

        //db setup
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SHOW TABLES;");

        //method call
        databaseConnection.initDatabase();

        //assertions
        assertEquals(2, resultSet.getMetaData().getColumnCount());

        if (resultSet.next()) {
            assertEquals("MESSAGES", resultSet.getString(1));
        }
        if (resultSet.next()) {
            assertEquals("MESSAGE_IMAGES", resultSet.getString(1));
        }
    }

    @AfterAll
    public static void tearDown() throws SQLException {
        connection.close();
    }
}