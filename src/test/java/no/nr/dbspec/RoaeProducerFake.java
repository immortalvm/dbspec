package no.nr.dbspec;

import java.nio.file.Path;
import java.util.Map;

public class RoaeProducerFake implements RoaeProducer {
    private final Database database;

    public RoaeProducerFake(Database database) {
        this.database = database;
    }

    @Override
    public void updateMetadata(String roaeFilename, MdObject mdo, Log log, Path dir) {
        database.trace(Map.of(
                "type", "roae",
                "filename", roaeFilename,
                "mdo", mdo.toString()));
    }
}
