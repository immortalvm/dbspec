package no.nr.dbspec;

public class AstError extends Error {
    private static final long serialVersionUID = 1L;
    Node node;

    AstError(Node node) {
        this.node = node;
    }
}
