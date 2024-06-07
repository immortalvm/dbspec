package no.nr.dbspec;

import java.io.IOException;
import java.nio.file.Path;

public interface RoaeProducer {
    void updateMetadata(String roaeFilename, MdObject mdo, Log log, Path dir) throws IOException;
}
