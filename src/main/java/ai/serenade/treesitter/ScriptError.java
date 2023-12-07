package ai.serenade.treesitter;

public class ScriptError extends Error {
	private static final long serialVersionUID = 1L;
	String interpreter;
	
	public ScriptError(String interpreter) {
		this.interpreter = interpreter;
	}
}
