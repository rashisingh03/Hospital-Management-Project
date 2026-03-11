import java.sql.*;

public class AddActiveColumn {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:hospital.db")) {
            Statement st = conn.createStatement();
            st.execute("ALTER TABLE users ADD COLUMN active INTEGER DEFAULT 1");
            st.close();
            System.out.println("'active' column added successfully!");
        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate column")) {
                System.out.println("'active' column already exists, skipping...");
            } else {
                e.printStackTrace();
            }
        }
    }
}
