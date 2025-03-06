package no.nr.dbspec;

import java.sql.Connection;
import java.util.Map;

public class SiardMetadataAdjusterFake implements SiardMetadataAdjuster {
    private final Database database;

    public SiardMetadataAdjusterFake(Database database) {
        this.database = database;
    }

    @Override
    public void updateMetadata(String siardFilename, SiardMd mdo, Connection connection) {
        database.trace(Map.of(
                "type", "adjustment",
                "filename", siardFilename,
                "mdo", mdo.toString(),
                "url", Database.getUrl(connection)));
    }
}
