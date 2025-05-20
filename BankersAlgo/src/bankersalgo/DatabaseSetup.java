package bankersalgo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {

    public static void setupDatabase() {
        String dbName = "oldehearth_db";
        String user = "root";
        String password = "123";

        // Step 1: Create database if it doesn't exist
        try (Connection conn = DriverManager.getConnection(
                     "jdbc:mysql://localhost:3306/?user=" + user + "&password=" + password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            System.out.println("✓ Database '" + dbName + "' checked/created.");

        } catch (SQLException e) {
            System.err.println("✗ Failed to create database: " + e.getMessage());
            return;
        }

        // Step 2: Create tables inside the actual database
        try (Connection conn = DriverManager.getConnection(
                     "jdbc:mysql://localhost:3306/" + dbName, user, password);
             Statement stmt = conn.createStatement()) {

            // Guests table
            String createGuests = """
                CREATE TABLE IF NOT EXISTS guests (
                    guest_id INT AUTO_INCREMENT PRIMARY KEY,
                    guest_name VARCHAR(100) NOT NULL,
                    allocated_regular INT DEFAULT 0,
                    allocated_deluxe INT DEFAULT 0,
                    allocated_staff INT DEFAULT 0,
                    max_regular INT DEFAULT 0,
                    max_deluxe INT DEFAULT 0,
                    max_staff INT DEFAULT 0,
                    status ENUM('Check-In', 'Check-Out') NOT NULL DEFAULT 'Check-In'             
                );
                """;
            stmt.executeUpdate(createGuests);

            // Resource Allocation table
            String createResource = """
                CREATE TABLE IF NOT EXISTS resource_allocation (
                    resource_type VARCHAR(50) PRIMARY KEY,
                    max_capacity INT NOT NULL,
                    allocated INT DEFAULT 0,
                    available INT NOT NULL,
                    status VARCHAR(10) DEFAULT 'safe'
                );
                """;
            stmt.executeUpdate(createResource);

            // Admin Users table
            String createAdminUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(100) NOT NULL
                );
                """;
            stmt.executeUpdate(createAdminUsers);

            // Create guest_requests table
                String createRequestsTable = """
                    CREATE TABLE IF NOT EXISTS guest_requests (
                        request_id INT AUTO_INCREMENT PRIMARY KEY,
                        guest_name VARCHAR(100) NOT NULL,
                        need_regular INT DEFAULT 0,
                        need_deluxe INT DEFAULT 0,
                        need_staff INT DEFAULT 0,
                        req_regular INT DEFAULT 0,
                        req_deluxe INT DEFAULT 0,
                        req_staff INT DEFAULT 0,
                        status ENUM('Approved', 'Denied', 'Pending', 'Invalid', '—') DEFAULT '—',
                        request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                    """;
                stmt.executeUpdate(createRequestsTable);

            // Insert default resources
            String insertResources = """
                INSERT IGNORE INTO resource_allocation (resource_type, max_capacity, allocated, available, status)
                VALUES 
                ('regular_suite', 20, 0, 20, 'safe'),
                ('deluxe_suite', 15, 0, 15, 'safe'),
                ('house_staff', 40, 0, 40, 'safe');
                """;
            stmt.executeUpdate(insertResources);

            // Insert default admin user (if not already exists)
            String insertAdmin = """
                INSERT IGNORE INTO users (username, password)
                VALUES ('admin', '123');
                """;
            stmt.executeUpdate(insertAdmin);

            System.out.println("Tables and initial data  setup complete.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to set up tables: " + e.getMessage());
        }
    }
}
