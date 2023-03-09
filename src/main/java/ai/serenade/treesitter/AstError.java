package ai.serenade.treesitter;

/**
 * In theory, this error should never be thrown. If it does get thrown, it
 * indicates a mismatch between the parser and the interpreter, so it may be
 * useful for debugging purposes.
 */
public class AstError extends Error {
	private static final long serialVersionUID = 1L;
}
