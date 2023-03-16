package ai.serenade.treesitter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ResultSet {
	List<ResultRow> rows;
	
	public ResultSet() {
		this.rows = new ArrayList<ResultRow>();
	}
	
	public void add(ResultRow row) {
		rows.add(row);
	}

	public String toString() {
		return rows.stream().map(r -> r.toString()).collect(Collectors.joining(", ", "(", ")"));
	}
	
	public List<ResultRow> getRows() {
		return rows;
	}

	public BigInteger getSize() {
		return BigInteger.valueOf(rows.size());
	}
	
}
