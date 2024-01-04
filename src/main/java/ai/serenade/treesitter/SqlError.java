package ai.serenade.treesitter;

public class SqlError extends Error {
	String reason;
    Node node;

	public SqlError(Node node, String reason) {
        this.node = node;
		this.reason = reason;
	}

}
