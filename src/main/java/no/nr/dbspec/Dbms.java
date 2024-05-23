package no.nr.dbspec;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Dbms {
    Map<Connection,Properties> connectionParameters;

    public Dbms() {
        connectionParameters = new HashMap<Connection,Properties>();
    }

    public Connection connect(String url, Context ctx) throws SQLException {
        Properties parameters = new Properties();
        ctx.forEach(parameters::put);
        Connection c = DriverManager.getConnection(url, parameters);
        connectionParameters.put(c, parameters);
        return c;
    }

    public int executeSqlUpdate(Connection connection, String sql) throws SQLException {
        Statement s = connection.createStatement();
        return s.executeUpdate(sql);
    }

    public ResultSet executeSqlQuery(Connection connection, String sql) throws SQLException {
        Statement s = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        return s.executeQuery(sql);
    }
}
