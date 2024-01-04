package ai.serenade.treesitter;

public class ScriptError extends Error {
	private static final long serialVersionUID = 1L;
	String interpreter;
    Node node;
	
	public ScriptError(Node node, String interpreter) {
        this.node = node;
		this.interpreter = interpreter;
	}
}
