package no.nr.dbspec;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class Database implements AutoCloseable {

    public final Connection connection;

    private final PreparedStatement insertStatement;
    private final PreparedStatement truncateStatement;

    public Database(String connectionString, String user, String password) throws SQLException {
        String url = connectionString + ";DB_CLOSE_DELAY=0;INIT=CREATE TABLE IF NOT EXISTS trace " +
                "(id int AUTO_INCREMENT NOT NULL, data json NOT NULL, PRIMARY KEY (id))";
        connection = DriverManager.getConnection(url, user, password);
        insertStatement = connection.prepareStatement(
                "INSERT INTO trace (`data`) VALUES (? FORMAT JSON)",
                new String[]{"id"});
        insertStatement.getGeneratedKeys();
        truncateStatement = connection.prepareStatement("TRUNCATE TABLE trace RESTART IDENTITY");
    }

    public void resetTrace() throws SQLException {
        truncateStatement.executeUpdate();
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    public int trace(Map<String, String> map) {
        return trace(new JSONObject(map));
    }

    public int trace(JSONObject obj) {
        return trace(obj.toString());
    }

    private int trace(String jsonString) {
        try {
            insertStatement.setString(1, jsonString);
            int res = insertStatement.executeUpdate();
            assert res == 1;
            ResultSet keys = insertStatement.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            } else {
                throw new SQLException("No ID obtained.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUrl(Connection connection) {
        try {
            return connection.getMetaData().getURL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
