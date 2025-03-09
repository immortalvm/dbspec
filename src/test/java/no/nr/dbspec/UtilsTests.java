package no.nr.dbspec;

import org.junit.jupiter.api.Test;

import static no.nr.dbspec.Utils.lines;
import static org.junit.jupiter.api.Assertions.*;

public class UtilsTests {

    @Test
    void test_lines() {
        assertArrayEquals(new String[]{""}, lines("")); // NB
        assertArrayEquals(new String[]{"abc"}, lines("abc"));
        assertArrayEquals(new String[]{"abc"}, lines("abc\n"));
        assertArrayEquals(new String[]{"abc", "def"}, lines("abc\ndef"));
        assertArrayEquals(new String[]{"abc", "def"}, lines("abc\r\ndef\r\n"));
        assertArrayEquals(new String[]{"abc", "", "def", ""}, lines("abc\n\ndef\n\n"));
        assertArrayEquals(new String[]{"abc\r"}, lines("abc\r"));
        assertArrayEquals(new String[]{"", "abc"}, lines("\nabc"));
    }
}
