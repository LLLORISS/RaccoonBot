package nm.rc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseControl {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    private static HikariDataSource dataSource;

    static {
        loadDBConfig();

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
        String query = "INSERT INTO users (id, username, name, lastname, words, userID) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.setString(2, username);
            statement.setString(3, name);
            statement.setString(4, lastname);
            statement.setInt(5, words);
            statement.setString(6, userID);
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

    private static void loadDBConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseControl.class.getClassLoader().getResourceAsStream("RaccoonConfig.properties")) {
            if (inputStream == null) {
                throw new IOException("Файл конфігурації 'RaccoonConfig.properties' не знайдено");
            }
            properties.load(inputStream);
            DB_URL = properties.getProperty("db.DB_URL");
            DB_USER = properties.getProperty("db.DB_USER");
            DB_PASSWORD = properties.getProperty("db.DB_PASSWORD");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Помилка при завантаженні конфігурації", e);
        }
    }
}