package ai.serenade.treesitter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import ch.admin.bar.siard2.cmd.SiardConnection;

public class Dbms {

	public static Connection connect(String url, Context ctx) {
		Properties parameters = new Properties();
		parameters.put("user", ctx.getValue("user"));
		parameters.put("password", ctx.getValue("password"));
		try {
			// The following is needed to load the custom SIARD versions of the JDBC drivers
			SiardConnection.getSiardConnection().loadDriver(url);		
			Connection c = DriverManager.getConnection(url, parameters);
			return c;
		} catch (SQLException e) {
			return null;
		}
	}

	public static int executeSqlUpdate(Connection connection, String sql) {
		try {
			Statement s = connection.createStatement();
			return s.executeUpdate(sql);
		} catch (SQLException e) {
			return 0;
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
