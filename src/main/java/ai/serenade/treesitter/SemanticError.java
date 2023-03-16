package ai.serenade.treesitter;

public class SemanticError extends Error {
	String reason;
	
	public SemanticError(String reason) {
		this.reason = reason;
	}
	
}
