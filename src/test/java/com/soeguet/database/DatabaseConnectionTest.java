package com.soeguet.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {

    @Test
    @DisplayName("Test if database environment variables are set")
    void testIfDatabaseEnvironmentVariablesAreSet() {

        //setup
        Properties properties = new Properties();
        DatabaseConnection databaseConnection = new DatabaseConnection(properties);

        //method call
        databaseConnection.setDatabaseSettings();

        //assertions
        assertTrue(databaseConnection.getDbPath().contains("jdbc:postgresql:"));
        assertFalse(databaseConnection.getProperties().getProperty("user").isBlank());
        assertFalse(databaseConnection.getProperties().getProperty("password").isBlank());
    }

    @Test
    @DisplayName("Test if database environment variables are set when all env vars are present")
    void setDatabaseSettingsAllEnvVarsPresent() {

        //setup
        Properties properties = new Properties();
        DatabaseConnection databaseConnection = Mockito.spy(new DatabaseConnection(properties));

        Mockito.when(databaseConnection.retrieveEnvironmentVariables("DB_URL")).thenReturn(java.util.Optional.of("jdbc:postgresql://localhost:5432/postgres"));
        Mockito.when(databaseConnection.retrieveEnvironmentVariables("DB_USER")).thenReturn(java.util.Optional.of("postgres"));
        Mockito.when(databaseConnection.retrieveEnvironmentVariables("DB_PASSWORD")).thenReturn(java.util.Optional.of("postgres"));

        //method call
        databaseConnection.setDatabaseSettings();

        //assertions
        assertEquals("jdbc:postgresql://localhost:5432/postgres", databaseConnection.getDbPath());
        assertEquals("postgres", databaseConnection.getProperties().getProperty("user"));
        assertEquals("postgres", databaseConnection.getProperties().getProperty("password"));
    }
}