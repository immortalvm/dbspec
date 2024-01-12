package ai.serenade.treesitter;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.Map;
import java.util.stream.Collectors;

public class Context {
	boolean selfEvaluating;
	Map<String,Object> bindings;
	Context base;
	
	public Context() {
		this.selfEvaluating = false;
		this.bindings = new HashMap<String,Object>();
		this.base = null;
	}
	
	public Context(Context base, boolean selfEvaluate) {
		this.selfEvaluating = selfEvaluate;
		this.bindings = null;
		this.base = base;
	}
	
	public void setValue(String name, Object value) {
		if (base != null) {
			base.setValue(name, value);
		} else {
			bindings.put(name, value);
		}
	}

	public void clearValue(String name) {
		if (base != null) {
			base.clearValue(name);
		} else {
			bindings.remove(name);
		}
	}

	public Object getValue(String name) {
		if (selfEvaluating) {
			return String.format("${%s}", name);
		} else if (base != null) {
			return base.getValue(name);
		} else {
			return bindings.get(name);
		}
	}
	
	public String toString() {
		return bindings.entrySet().stream().map(e -> String.format("%s='%s'", e.getKey(), e.getValue())).collect(Collectors.joining(", ", "[", "]"));
	}

    public void forEach(BiConsumer<String, Object> action) {
        bindings.forEach((k, v) -> action.accept(k, getValue(k)));
    }
}
