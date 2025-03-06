package no.nr.dbspec;

import org.treesitter.TSNode;

public class AstFailure extends RuntimeException {
    TSNode node;

    AstFailure(TSNode node) {
        this.node = node;
    }
}
