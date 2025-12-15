package no.nr.dbspec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Dbms {
    Map<Connection,Properties> connectionParameters;
    private final TimingContext timingContext;

    public Dbms(TimingContext timingContext) {
        connectionParameters = new HashMap<Connection,Properties>();
        this.timingContext = timingContext;
    }

    public Connection connect(String url, NormalContext ctx) throws SQLException {
        Properties parameters = new Properties();
        ctx.forEach(parameters::put);
        Connection c = DriverManager.getConnection(url, parameters);
        connectionParameters.put(c, parameters);
        return c;
    }

    public int executeSqlUpdate(Connection connection, Map.Entry<String, List<Object>> pair) throws SQLException {
        long startTime = System.nanoTime();
        try {
            return getPreparedStatement(connection, pair).executeUpdate();
        } finally {
            if (timingContext != null) {
                timingContext.addSqlTime(System.nanoTime() - startTime);
            }
        }
    }

    public ResultSet executeSqlQuery(Connection connection, Map.Entry<String, List<Object>> pair) throws SQLException {
        long startTime = System.nanoTime();
        try {
            return getPreparedStatement(connection, pair).executeQuery();
        } finally {
            if (timingContext != null) {
                timingContext.addSqlTime(System.nanoTime() - startTime);
            }
        }
    }

    private static PreparedStatement getPreparedStatement(
            Connection connection,
            Map.Entry<String, List<Object>> pair) throws SQLException {
        String sql = pair.getKey();
        List<Object> args = pair.getValue();
        PreparedStatement ps = connection.prepareStatement(
                sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        int i = 1;
        for (Object x : args) {
            // TODO: Should we also support other types?
            if (x instanceof String) setNString(ps, i, (String)x);
            else if (x instanceof BigInteger) ps.setBigDecimal(i, new BigDecimal((BigInteger)x));
            else throw new SQLException("Only string and integer arguments are currently supported.");
            i++;
        }
        return ps;
    }

    private static void setNString(PreparedStatement ps, int parameterIndex, String value) throws SQLException {
        try {
            ps.setNString(parameterIndex, value);
        } catch (SQLException e) {
            // Since PostgreSQL JDBC driver does not have NCHAR or NVARCHAR.
            // TODO: Can we do better than this?
            ps.setString(parameterIndex, value);
        }
    }
}
