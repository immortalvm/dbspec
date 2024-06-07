package no.nr.dbspec;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class SiardExtractorFake implements SiardExtractor {
    private final Database database;

    public SiardExtractorFake(Database database) {
        this.database = database;
    }

    @Override
    public void transfer(Connection conn, String siardFilename) {
        database.trace(Map.of(
                "type", "extraction",
                "url", Database.getUrl(conn),
                "filename", siardFilename));
    }
}
