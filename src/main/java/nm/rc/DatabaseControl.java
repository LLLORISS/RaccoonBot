package nm.rc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class DatabaseControl {
    private final static String DB_URL = System.getenv("DB_URL");
    private final static String DB_USER = System.getenv("DB_USER");
    private final static String DB_PASSWORD = System.getenv("DB_PASSWORD");

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setMaximumPoolSize(10);
        config.setPoolName("RaccoonPool");
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static int getUserCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM users";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    public static boolean userExist(String userId) throws SQLException {
        String query = "SELECT 1 FROM users WHERE userid = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public static boolean insertUser(int id, String username, String name, String lastname, int words, String userID) throws SQLException {
        String query = "INSERT INTO users (id, username, name, lastname, words, userID, lastupdate) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.setString(2, username);
            statement.setString(3, name);
            statement.setString(4, lastname);
            statement.setInt(5, words);
            statement.setString(6, userID);
            statement.setString(7, getCurrentTime());
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    public static String getTopUsers() throws SQLException {
        String query = "SELECT username, words FROM users ORDER BY words DESC LIMIT 10";
        StringBuilder result = new StringBuilder("Топ 10 гравців:\n");

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            int id = 1;
            int count = 0;
            while (resultSet.next() && count < 10) {
                String username = resultSet.getString("username");

                if ("GroupAnonymousBot".equals(username)) {
                    continue;
                }

                int words = resultSet.getInt("words");
                result.append(id).append(". @").append(username).append(" Відгаданих слів: ").append(words).append("\n");
                id++;
                count++;
            }
        }

        return result.toString();
    }

    public static void increaseWords(String userID){
        String query = "UPDATE users SET words = words + 1 WHERE userID = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userID);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Words successfully incremented for user: " + userID);
            } else {
                System.out.println("User not found: " + userID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateInfo(String userID, String username, String name, String lastname) {
        String updateQuery = "UPDATE users SET username = ?, name = ?, lastname = ?, lastupdate WHERE user_id = ?";

        try (Connection connection = DatabaseControl.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, lastname);
            preparedStatement.setString(4, getCurrentTime());
            preparedStatement.setString(4, userID);

            int rowsUpdated = preparedStatement.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("[RaccoonBot] Info has been successfully updated for user with ID: " + userID);
            } else {
                System.out.println("User with ID: " + userID + " not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasOneDayPassed(String userID) throws SQLException {
        LocalDateTime lastUpdate = getLastUpdate(userID);

        LocalDateTime currentTime = LocalDateTime.now();

        assert lastUpdate != null;
        Duration duration = Duration.between(lastUpdate, currentTime);

        return duration.toDays() >= 1;
    }

    private static LocalDateTime getLastUpdate(String userID) throws SQLException {
        String query = "SELECT lastupdate FROM users WHERE userID = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userID);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String lastUpdateStr = resultSet.getString("lastupdate");
                    return LocalDateTime.parse(lastUpdateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } else {
                    return null;
                }
            }
        }
    }

    public static void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            try {
                if (resource != null) {
                    resource.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String getCurrentTime() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}