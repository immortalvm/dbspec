package ai.serenade.treesitter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Context {
	Map<String,Object> bindings;
	
	public Context() {
		this.bindings = new HashMap<String,Object>();
	}
	
	public void setValue(String name, Object value) {
		bindings.put(name, value);
	}

	public Object getValue(String name) {
		return bindings.get(name);
	}
	
	public String toString() {
		return bindings.entrySet().stream().map(e -> String.format("%s='%s'", e.getKey(), e.getValue())).collect(Collectors.joining(", ", "[", "]"));
	}
		
}
