package ai.serenade.treesitter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Context {
	Map<String,Object> bindings;
	Context base;
	
	public Context() {
		this.bindings = new HashMap<String,Object>();
		this.base = null;
	}
	
	public Context(Context base) {
		this.bindings = new HashMap<String,Object>();
		this.base = base;
	}
	
	public void setValue(String name, Object value) {
		Context ctx = findContext(name);
		if (ctx != null) {
			ctx.bindings.put(name, value);
		} else {
			bindings.put(name, value);
		}
	}

	public Object getValue(String name) {
		Context ctx = findContext(name);
		return ctx != null ? ctx.bindings.get(name) : null;
	}
	
	public String toString() {
		return bindings.entrySet().stream().map(e -> String.format("%s='%s'", e.getKey(), e.getValue())).collect(Collectors.joining(", ", "[", "]"));
	}

	Context findContext(String name) {
		if (bindings.get(name) != null) {
			return this;
		} else if (base != null) {
			return base.findContext(name);
		} else {
			return null;
		}
	}
		
}
