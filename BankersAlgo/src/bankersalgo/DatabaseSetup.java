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
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS oldeHearth_db");
            stmt.executeUpdate("USE oldeHearth_db");

            // Create users table
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(255) NOT NULL, "
                    + "usertype VARCHAR(50) NOT NULL, "
                    + "password VARCHAR(255) NOT NULL);";
            stmt.executeUpdate(createUsersTable);

            // Insert default admin if not exists
            String checkAdminSQL = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            PreparedStatement checkStmt = conn.prepareStatement(checkAdminSQL);
            var rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO users (username, usertype, password) VALUES ('admin', 'admin', '123')");
            }

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

            // Insert into resource_allocation if empty
            String insertResourceSQL = "INSERT IGNORE INTO resource_allocation (id, resource_type, max_capacity, allocated, available, status) VALUES "
                    + "(1, 'regular_suite', 20, 0, 20, 'safe'), "
                    + "(2, 'deluxe_suite', 12, 0, 12, 'safe'), "
                    + "(3, 'house_staff', 20, 0, 20, 'safe');";
            stmt.executeUpdate(insertResourceSQL);

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

            // Sample archived records
            String insertArchived = "INSERT IGNORE INTO archived_records (id, guest_name, room_type, check_in_time, check_out_time, check_in_date, check_out_date, payment_status) VALUES "
                    + "(1, 'John Doe', 'deluxe', '2025-05-10 14:00:00', '2025-05-12 10:00:00', '2025-05-10', '2025-05-12', 'paid'), "
                    + "(2, 'Jane Smith', 'regular', '2025-05-05 15:00:00', '2025-05-07 12:00:00', '2025-05-05', '2025-05-07', 'pending');";
            stmt.executeUpdate(insertArchived);

            System.out.println("Database setup completed successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
