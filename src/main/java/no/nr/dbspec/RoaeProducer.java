package no.nr.dbspec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface RoaeProducer {
    void updateMetadata(Path path, List<RoaeMd> commands) throws IOException;
}
