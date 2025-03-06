package no.nr.dbspec;

import org.treesitter.TSNode;

public class SqlFailure extends RuntimeException {
    String reason;
    TSNode node;

    public SqlFailure(TSNode node, String reason) {
        this.node = node;
        this.reason = reason;
    }
}
