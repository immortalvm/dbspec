package no.nr.dbspec;

import java.sql.SQLException;

public class StringRows implements Rows {

    private final String[] rows;
    int pos = -1;

    public StringRows(String str) {
        this.rows = Utils.lines(str);
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
        return pos < rows.length ? new String[]{rows[pos++]} : null;
    }
}
