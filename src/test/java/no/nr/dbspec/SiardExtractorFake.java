package no.nr.dbspec;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Map;

public class SiardExtractorFake implements SiardExtractor {
    private final Database database;

    public SiardExtractorFake(Database database) {
        this.database = database;
    }

    @Override
    public void transfer(Connection conn, Path path) {
        database.trace(Map.of(
                "type", "extraction",
                "url", Database.getUrl(conn),
                "filename", path.getFileName().toString()));
    }
}
