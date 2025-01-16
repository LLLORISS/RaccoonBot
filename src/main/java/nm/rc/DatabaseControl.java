package nm.rc;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseControl {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    static {
        loadDBConfig();

        System.out.println(DB_URL);
        System.out.println(DB_USER);
        System.out.println(DB_PASSWORD);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static int getUserCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM users";
        try (ResultSet resultSet = executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public static boolean userExist(String userId) throws SQLException {
        String query = "SELECT 1 FROM users WHERE userid = ?";
        try (ResultSet resultSet = executeQuery(query, userId)) {
            return resultSet.next();
        }
    }

    public static boolean insertUser(int id, String username, String name, String lastname, int words, String userID) throws SQLException {
        String query = "INSERT INTO users (id, username, name, lastname, words, userID) VALUES (?, ?, ?, ?, ?, ?)";
        int rowsAffected = executeUpdate(query, id, username, name, lastname, words, userID);
        return rowsAffected > 0;
    }

    public static ResultSet executeQuery(String query, Object... params) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(query);
        setParameters(statement, params);
        return statement.executeQuery();
    }

    public static int executeUpdate(String query, Object... params) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            return statement.executeUpdate();
        }
    }

    private static void setParameters(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    public static void closeResources(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        try {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
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
