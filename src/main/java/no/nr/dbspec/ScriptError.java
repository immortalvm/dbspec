package no.nr.dbspec;

public class ScriptError extends Error {
    private static final long serialVersionUID = 1L;
    Node node;
    String reason;

    public ScriptError(Node node, String reason) {
        this.node = node;
        this.reason = reason;
    }
}
