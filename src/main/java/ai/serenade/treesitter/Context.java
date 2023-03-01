package ai.serenade.treesitter;

public class Context {
	String name;
	Object value;
	Context next;
	
	public Context(String n, Context c) {
		name = n;
		value = null;
		next = c;
	}
	
	public boolean setValue(String n, Object v, Context c) {
		Context cc = find(n, c);
		if (cc != null) {
			cc.value = v;
			return true;
		} else {
			return false;
		}
	}

	public Object getValue(String name, Context c) {
		Context cc = find(name, c);
		return cc == null ? null : cc.value;
	}
		
	Context find(String n, Context c) {
		if (n.equals(name)) {
			return this;
		} else if (next == null) {
			return null;
		} else {
			return find(n, next);
		}
	}
	
}
