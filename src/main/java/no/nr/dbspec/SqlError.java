package no.nr.dbspec;

import org.treesitter.TSNode;

public class SqlError extends Error {
    String reason;
    TSNode node;

    public SqlError(TSNode node, String reason) {
        this.node = node;
        this.reason = reason;
    }
}
