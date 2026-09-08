package com.minispl.persistence;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager manages SQLite JDBC connections and schema initialization.
 */
public class DatabaseManager {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:dfir_orchestrator.db";
    private static String dbUrl = DEFAULT_DB_URL;
    private static DatabaseManager instance;

    private DatabaseManager() {
        // Enforce SQLite JDBC driver loading
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found on classpath", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public static synchronized void setDbUrl(String newDbUrl) {
        dbUrl = newDbUrl;
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Executes the schema.sql script to create tables and indexes.
     */
    public void initializeDatabase() {
        try (Connection conn = getConnection()) {
            InputStream is = getClass().getResourceAsStream("/schema.sql");
            if (is == null) {
                // Fallback to classloader
                is = DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql");
            }

            if (is == null) {
                throw new IllegalStateException("schema.sql could not be found in resources");
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Ignore SQL comments and empty lines
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                        sb.append(line).append("\n");
                    }
                }
            }

            String[] statements = sb.toString().split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql.trim());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }
}
