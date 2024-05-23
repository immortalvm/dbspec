package no.nr.dbspec;

import org.treesitter.TSNode;

public class ScriptError extends Error {
    private static final long serialVersionUID = 1L;
    TSNode node;
    String reason;

    public ScriptError(TSNode node, String reason) {
        this.node = node;
        this.reason = reason;
    }
}
