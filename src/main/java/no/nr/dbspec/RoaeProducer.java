package no.nr.dbspec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface RoaeProducer {
    void updateMetadata(String roaeFilename, List<CommandMd> commands, Log log, Path dir) throws IOException;
}
