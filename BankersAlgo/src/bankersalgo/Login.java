package bankersalgo;

import javax.swing.JOptionPane;
import java.sql.*;

public class Login {

    public boolean validateLogin(String username, String password) {
        boolean isValid = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Get connection from the utility class
            conn = DatabaseConnection.getConnection();
            // SQL query to check if the user exists in the database
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            rs = stmt.executeQuery();

            // If a record is found, it means login is successful
            if (rs.next()) {
                isValid = true;
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Database connection error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Close the resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return isValid;
    }
}
