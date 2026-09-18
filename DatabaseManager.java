import java.sql.*;
import java.util.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    static {
        try {
            Class.forName("org.sqlite.JDBC");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:chat.db");
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000);
            config.setIdleTimeout(60000);
            config.setMaxLifetime(180000);

            dataSource = new HikariDataSource(config);
            createTables();
            System.out.println("[DB] SQLite + HikariCP initialized");
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void createTables() throws SQLException {
        String userSchema = "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "clientId INTEGER UNIQUE NOT NULL, " +
            "joinedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String messageSchema = "CREATE TABLE IF NOT EXISTS messages (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "clientId INTEGER NOT NULL, " +
            "messageText TEXT NOT NULL, " +
            "sentAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (clientId) REFERENCES users(clientId))";

        String indexSchema = "CREATE INDEX IF NOT EXISTS idx_messages_sentAt ON messages(sentAt DESC)";

        try (
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement()
        ) {
            stmt.execute(userSchema);
            stmt.execute(messageSchema);
            stmt.execute(indexSchema);
        }
    }

    public static void registerUser(int clientId) {
        String sql = "INSERT OR IGNORE INTO users (clientId) VALUES (?)";
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setInt(1, clientId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error registering user: " + e.getMessage());
        }
    }

    public static void saveMessage(int clientId, String messageText) {
        String sql = "INSERT INTO messages (clientId, messageText) VALUES (?, ?)";
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setInt(1, clientId);
            pstmt.setString(2, messageText);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error saving message: " + e.getMessage());
        }
    }

    public static List<String> getChatHistory(int limit) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT clientId, messageText FROM messages " +
            "ORDER BY sentAt DESC LIMIT ?";

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int clientId = rs.getInt("clientId");
                    String messageText = rs.getString("messageText");
                    history.add(0, "[Client #" + clientId + "] " + messageText);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error fetching history: " + e.getMessage());
        }
        return history;
    }
}