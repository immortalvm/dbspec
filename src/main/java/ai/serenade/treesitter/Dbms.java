package ai.serenade.treesitter;

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
	
	public Connection connect(String url, Context ctx) {
		Properties parameters = new Properties();
		parameters.put("user", ctx.getValue("user"));
		parameters.put("password", ctx.getValue("password"));
		try {
			Connection c = DriverManager.getConnection(url, parameters);
			connectionParameters.put(c, parameters);
			return c;
		} catch (SQLException e) {
			return null;
		}
	}

	public int executeSqlUpdate(Connection connection, String sql) {
		try {
			Statement s = connection.createStatement();
			return s.executeUpdate(sql);
		} catch (SQLException e) {
			return 0;
		}
	}

	public ResultSet executeSqlQuery(Connection connection, String sql) {
		try {
			Statement s = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			return s.executeQuery(sql);
		} catch (SQLException e) {
			return null;
		}
	}

}
