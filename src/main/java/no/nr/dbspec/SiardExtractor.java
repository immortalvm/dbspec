package no.nr.dbspec;

import java.sql.Connection;

public interface SiardExtractor {
    void transfer(Connection conn, String siardFilename) throws SiardError;
}
