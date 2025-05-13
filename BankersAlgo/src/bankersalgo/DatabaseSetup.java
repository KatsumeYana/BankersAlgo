package bankersalgo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {

    public static void setupDatabase() {
        Connection conn = null;
        Statement stmt = null;

        try {
            // First connect to server without specifying database
            conn = DatabaseConnection.getServerConnection();
            stmt = conn.createStatement();
            
            // Create database if not exists
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS oldehearth_db");
            stmt.close();
            conn.close();

            // Now connect to the specific database
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();

            // Create users table
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(255) NOT NULL UNIQUE, "
                    + "usertype VARCHAR(50) NOT NULL, "
                    + "password VARCHAR(255) NOT NULL);";
            stmt.executeUpdate(createUsersTable);

            // Insert default admin if not exists using proper prepared statement
            String checkAdminSQL = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            PreparedStatement checkStmt = conn.prepareStatement(checkAdminSQL);
            var rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                String insertAdminSQL = "INSERT INTO users (username, usertype, password) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertAdminSQL);
                insertStmt.setString(1, "admin");
                insertStmt.setString(2, "admin");
                insertStmt.setString(3, "123");
                insertStmt.executeUpdate();
                insertStmt.close();
            }
            checkStmt.close();

            // Create guests table with allocation fields
            String createGuestsTable = "CREATE TABLE IF NOT EXISTS guests ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "guest_name VARCHAR(255) NOT NULL, "
                    + "allocated_regular INT DEFAULT 0, "
                    + "allocated_deluxe INT DEFAULT 0, "
                    + "allocated_staff INT DEFAULT 0, "
                    + "max_regular INT NOT NULL, "
                    + "max_deluxe INT NOT NULL, "
                    + "max_staff INT NOT NULL, "
                    + "check_in_time DATETIME NOT NULL, "
                    + "check_out_time DATETIME DEFAULT NULL, "
                    + "payment_status ENUM('paid', 'pending', 'refund') DEFAULT 'pending', "
                    + "room_inspection_completed BOOLEAN DEFAULT FALSE);";
            stmt.executeUpdate(createGuestsTable);

            // Create resource_allocation table
            String createResourceTable = "CREATE TABLE IF NOT EXISTS resource_allocation ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "resource_type VARCHAR(255) NOT NULL, "
                    + "max_capacity INT NOT NULL, "
                    + "allocated INT NOT NULL, "
                    + "available INT NOT NULL, "
                    + "status ENUM('safe', 'warning', 'danger') NOT NULL);";
            stmt.executeUpdate(createResourceTable);

            // Insert into resource_allocation if empty using proper check
            String checkResourceSQL = "SELECT COUNT(*) FROM resource_allocation";
            checkStmt = conn.prepareStatement(checkResourceSQL);
            rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                String insertResourceSQL = "INSERT INTO resource_allocation (resource_type, max_capacity, allocated, available, status) VALUES "
                        + "('regular_suite', 20, 0, 20, 'safe'), "
                        + "('deluxe_suite', 12, 0, 12, 'safe'), "
                        + "('house_staff', 20, 0, 20, 'safe');";
                stmt.executeUpdate(insertResourceSQL);
            }
            checkStmt.close();

            // Create archived_records table
            String createArchivedTable = "CREATE TABLE IF NOT EXISTS archived_records ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "guest_name VARCHAR(255) NOT NULL, "
                    + "room_type ENUM('regular', 'deluxe') NOT NULL, "
                    + "check_in_time DATETIME NOT NULL, "
                    + "check_out_time DATETIME NOT NULL, "
                    + "check_in_date DATE NOT NULL, "
                    + "check_out_date DATE NOT NULL, "
                    + "payment_status ENUM('paid', 'pending', 'refund') DEFAULT 'pending');";
            stmt.executeUpdate(createArchivedTable);

            

            System.out.println("Database setup completed successfully.");
        } catch (SQLException e) {
            System.err.println("Error setting up database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing database resources: " + e.getMessage());
            }
        }
    }
}