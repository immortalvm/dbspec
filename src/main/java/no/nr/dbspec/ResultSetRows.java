package no.nr.dbspec;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetRows implements Rows {

    private final ResultSet resultSet;
    private final int columnCount;
    private int size = -1;
    private boolean locked = false;

    public ResultSetRows(ResultSet resultSet) throws SQLException {
        this.resultSet = resultSet;
        columnCount = resultSet.getMetaData().getColumnCount();
    }

    @Override
    public int getSize() throws SQLException {
        if (size == -1) {
            int currentRow = resultSet.getRow();
            resultSet.last();
            size = resultSet.getRow();
            resultSet.absolute(currentRow);
        }
        return size;
    }

    @Override
    public boolean tryLockAndRewind() throws SQLException
    {
        if (locked) {
            return false;
        }
        resultSet.beforeFirst();
        locked = true;
        return true;
    }

    @Override
    public void free() {
        locked = false;
    }

    @Override
    public String[] next() throws SQLException {
        if (resultSet.next()) {
            String[] row = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = resultSet.getString(i + 1);
            }
            return row;
        } else {
            return null;
        }
    }
}
