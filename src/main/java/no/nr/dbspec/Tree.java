// This file is based on ai.serenade.treesitter.Tree in java-tree-sitter.
package no.nr.dbspec;

public class Tree implements AutoCloseable {
    private long pointer;

    Tree(long pointer) {
        this.pointer = pointer;
    }

    @Override
    public void close() {
        TreeSitter.treeDelete(pointer);
    }

    public Node getRootNode() {
        return TreeSitter.treeRootNode(pointer);
    }
}
