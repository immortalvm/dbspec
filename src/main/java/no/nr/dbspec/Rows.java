package no.nr.dbspec;

import java.sql.SQLException;

public interface Rows {
    int getSize() throws SQLException;
    boolean tryLockAndRewind() throws SQLException;
    void free();
    String[] next() throws SQLException;
}
