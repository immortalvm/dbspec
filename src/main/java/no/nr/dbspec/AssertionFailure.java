package no.nr.dbspec;

import org.treesitter.TSNode;

public class AssertionFailure extends RuntimeException {
    TSNode node;

    public AssertionFailure(TSNode node) {
        this.node = node;
    }
}
