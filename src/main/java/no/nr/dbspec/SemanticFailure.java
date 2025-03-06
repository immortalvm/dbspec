package no.nr.dbspec;

import org.treesitter.TSNode;

public class SemanticFailure extends RuntimeException {
    String reason;
    TSNode node;

    public SemanticFailure(TSNode node, String reason) {
        this.node = node;
        this.reason = reason;
    }
}
