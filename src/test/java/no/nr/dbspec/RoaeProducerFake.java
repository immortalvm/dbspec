package no.nr.dbspec;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoaeProducerFake implements RoaeProducer {
    private final Database database;

    public RoaeProducerFake(Database database) {
        this.database = database;
    }

    @Override
    public void updateMetadata(Path path, List<RoaeMd> commands) {
        if (commands == null || commands.isEmpty()) return;
        String str = commands.stream().map(RoaeMd::toString).collect(Collectors.joining(" "));
        database.trace(Map.of(
                "type", "roae",
                "filename", path.getFileName().toString(),
                "mdo", str));
    }
}
