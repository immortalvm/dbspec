package ai.serenade.treesitter;

public class Query {

	// TODO: Do an actual query here; this just generates a synthetic result set for
	// testing purposes.

	public static ResultSet getResultSet(String sql) {
		ResultSet rows = new ResultSet();
		ResultRow row;

		row = new ResultRow();
		row.addString("1");
		row.addString("Alfa");
		rows.add(row);
		row = new ResultRow();
		row.addString("2");
		row.addString("Bravo");
		rows.add(row);
		row = new ResultRow();
		row.addString("3");
		row.addString("Charlie");
		rows.add(row);
		return rows;
	}

}
