package no.nr.dbspec;

import org.treesitter.TSNode;

public class AssertionFailure extends RuntimeException {
    final TSNode node;
    final Context context;

    public AssertionFailure(TSNode node, Context context) {
        this.node = node;
        this.context = context;
    }
}
