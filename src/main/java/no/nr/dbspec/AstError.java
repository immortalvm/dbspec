package no.nr.dbspec;

import org.treesitter.TSNode;

public class AstError extends Error {
    TSNode node;

    AstError(TSNode node) {
        this.node = node;
    }
}
