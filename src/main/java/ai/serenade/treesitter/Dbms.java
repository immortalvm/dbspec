package ai.serenade.treesitter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Dbms {

	public static Connection connect(String url, Context ctx) {
		Properties parameters = new Properties();
		parameters.put("user", ctx.getValue("user"));
		parameters.put("password", ctx.getValue("password"));
		try {
			Connection c = DriverManager.getConnection(url, parameters);
			return c;
		} catch (SQLException e) {
			return null;
		}
	}

	public static boolean executeSql(Connection connection, String sql) {
		try {
			Statement s = connection.createStatement();
			return s.execute(sql);
		} catch (SQLException e) {
			return false;
		}
	}

	public static ResultSet executeSqlQuery(Connection connection, String sql) {
		try {
			Statement s = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			return s.executeQuery(sql);
		} catch (SQLException e) {
			return null;
		}
	}

}
