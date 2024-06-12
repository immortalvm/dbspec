package no.nr.dbspec;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;

import no.nr.dbspec.RoaeMd.RoaeMdType;

public class RoaeProducerImpl implements RoaeProducer {
    @Override
    public void updateMetadata(Path path, List<RoaeMd> commands) throws IOException {
        PrintWriter output = new PrintWriter(new FileWriter(path.toFile()));
        for (RoaeMd cObj : commands) {
            output.write(cObj.getData());
            output.format("%s\n", cObj.getData());
            for (RoaeMd pObj : cObj.getChildren(RoaeMdType.PARAMETER)) {
                String description = pObj.getData();
                output.format("%s - %s\n", pObj.getName(), description == null ? "" : description);
            }
            for (RoaeMd sObj : cObj.getChildren(RoaeMdType.SQL)) {
                output.format("%s\n", sObj.getData());
            }
        }
        output.close();
    }
}
