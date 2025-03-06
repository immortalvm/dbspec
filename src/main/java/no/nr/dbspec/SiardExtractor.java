package no.nr.dbspec;

import java.nio.file.Path;
import java.sql.Connection;

public interface SiardExtractor {
    void transfer(Connection conn, Path path) throws SiardException;
}
