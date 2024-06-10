package no.nr.dbspec;

import org.treesitter.TSNode;

import java.sql.Connection;

public interface SiardMetadataAdjuster {
    void updateMetadata(String siardFilename, SiardMd mdo, Connection connection, TSNode n);
}
