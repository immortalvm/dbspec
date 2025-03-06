package no.nr.dbspec;

import java.sql.Connection;

public interface SiardMetadataAdjuster {
    void updateMetadata(String siardFilename, SiardMd mdo, Connection connection) throws SiardException;
}
