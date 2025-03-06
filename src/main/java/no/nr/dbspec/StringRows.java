package no.nr.dbspec;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class StringRows implements Rows {

    private static final Pattern tabPattern = Pattern.compile("\t");

    private final String[][] rows;
    int pos = -1;

    public StringRows(String str) {
        String[] lines = Utils.lines(str);
        this.rows = Arrays.stream(lines)
                .map(line -> tabPattern.split(line, -1))
                .toArray(String[][]::new);
    }

    @Override
    public int getSize() {
        return rows.length;
    }

    @Override
    public boolean tryLockAndRewind() {
        if (pos != -1) {
            return false;
        }
        pos = 0;
        return true;
    }

    @Override
    public void free() {
        pos = -1;
    }

    @Override
    public String[] next() throws SQLException {
        return pos < rows.length ? rows[pos++] : null;
    }
}
