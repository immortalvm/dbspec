package no.nr.dbspec;

import java.nio.file.Path;
import java.sql.Connection;

public interface SiardMetadataAdjuster {
    void updateMetadata(Path siardFilePath, SiardMd mdo, Connection connection) throws SiardException;
}
