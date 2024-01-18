package no.nr.dbspec;

public class AssertionFailure extends Error {
    private static final long serialVersionUID = 1L;
    Node node;

    public AssertionFailure(Node node) {
        this.node = node;
    }
}
