package no.nr.dbspec;

import org.treesitter.TSNode;

public class SemanticError extends Error {
    String reason;
    TSNode node;

    public SemanticError(TSNode node, String reason) {
        this.node = node;
        this.reason = reason;
    }
}
