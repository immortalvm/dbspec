package no.nr.dbspec;

public class SemanticError extends Error {
    String reason;
    Node node;

    public SemanticError(Node node, String reason) {
        this.node = node;
        this.reason = reason;
    }
}
