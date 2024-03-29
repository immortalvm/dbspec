// This file is based on ai.serenade.treesitter.Parser in java-tree-sitter.
package no.nr.dbspec;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class Parser implements AutoCloseable {
    private long pointer;

    Parser(long pointer) {
        this.pointer = pointer;
    }

    public Parser() {
        this(TreeSitter.parserNew());
    }

    public void setLanguage(long language) {
        TreeSitter.parserSetLanguage(pointer, language);
    }

    public Tree parseString(String source) throws UnsupportedEncodingException {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_16LE);
        return new Tree(TreeSitter.parserParseBytes(pointer, bytes, bytes.length));
    }

    @Override
    public void close() {
        TreeSitter.parserDelete(pointer);
    }
}
