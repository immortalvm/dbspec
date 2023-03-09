package ai.serenade.treesitter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ResultRow {
	List<String> tuple;
	
	public ResultRow() {
		this.tuple = new ArrayList<String>();
	}
	
	public void addString(String string) {
		tuple.add(string);
	}

	public String getString(int i) {
		return tuple.get(i);
	}
	
	public String toString() {
		return tuple.stream().collect(Collectors.joining(", ", "(", ")"));
	}
	
}
