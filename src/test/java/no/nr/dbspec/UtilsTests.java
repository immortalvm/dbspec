package no.nr.dbspec;

import org.junit.jupiter.api.Test;

import static no.nr.dbspec.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

public class UtilsTests {

    @Test
    void test_stripFinalNewline() {
        assertEquals("abc", stripFinalNewline("abc"));
        assertEquals("abc", stripFinalNewline("abc\n"));
        assertEquals("abc", stripFinalNewline("abc\r\n"));
        assertEquals("abc\n", stripFinalNewline("abc\n\n"));
        assertEquals("abc\r\n", stripFinalNewline("abc\r\n\r\n"));
    }

    @Test
    void test_prefixAndFixLineSeparators() {
        String p = "prefix";
        String lsp = System.lineSeparator() + p;
        assertEquals(p, prefixAndFixLineSeparators(p, ""));
        assertEquals(p + lsp, prefixAndFixLineSeparators(p, "\n"));
        assertEquals(
                p + "abc" + lsp + lsp + "def" + lsp,
                prefixAndFixLineSeparators(p, "abc\n\ndef\r\n"));
    }

    @Test
    void test_tsLineEndings() {
        assertArrayEquals(strings(), tsLineEndings(""));
        assertArrayEquals(strings("\n"), tsLineEndings("\n"));
        assertArrayEquals(strings("\r\n"), tsLineEndings("\r\n"));
        assertArrayEquals(strings(), tsLineEndings("\r"));
        assertArrayEquals(strings("\n", "\r\n"), tsLineEndings("\nabc\r\r\ndef"));
    }

    private static String[] strings(String... strings) {
        return strings;
    }
}
