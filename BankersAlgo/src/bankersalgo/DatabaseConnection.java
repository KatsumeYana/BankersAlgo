package bankersalgo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String BASE_URL = "jdbc:mysql://localhost:3306";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/oldehearth_db";
    private static final String USER = "root";
    private static final String PASSWORD = "123";

    // For initial connection without specifying database
    public static Connection getServerConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(BASE_URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // For normal connections after database is created
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
