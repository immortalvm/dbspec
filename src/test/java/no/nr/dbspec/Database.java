package no.nr.dbspec;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.Map;

public class Database implements AutoCloseable {

    public final Connection connection;

    private final PreparedStatement insertStatement;

    public Database(String connectionString) throws SQLException {
        connection = DriverManager.getConnection(
                connectionString + ";INIT=runscript from 'classpath:/init.sql'");
        insertStatement = connection.prepareStatement(
                "INSERT INTO trace (`data`) VALUES (? FORMAT JSON)",
                new String[] {"id"});
        insertStatement.getGeneratedKeys();
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    public int trace(Map<String, String> map) throws SQLException {
        return trace(new JSONObject(map));
    }

    public int trace(String... strings) throws SQLException {
        return trace(new JSONArray(strings));
    }

    public int trace(JSONArray arr) throws SQLException {
        return trace(arr.toString());
    }

    public int trace(JSONObject obj) throws SQLException {
        return trace(obj.toString());
    }

    private int trace(String jsonString) throws SQLException {
        insertStatement.setString(1, jsonString);
        int res = insertStatement.executeUpdate();
        assert res == 1;
        ResultSet keys = insertStatement.getGeneratedKeys();
        if (keys.next()) {
            return keys.getInt(1);
        } else {
            throw new SQLException("No ID obtained.");
        }
    }
}
