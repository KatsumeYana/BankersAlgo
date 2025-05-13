package bankersalgo;


public class Main {

    public static void main(String[] args) {
        // First, initialize the database and create necessary tables
//        DatabaseConnection.getConnection();
        DatabaseSetup.setupDatabase();

        // Then, launch the login form
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Login().setVisible(true);
            
            }
        });
    }
}
