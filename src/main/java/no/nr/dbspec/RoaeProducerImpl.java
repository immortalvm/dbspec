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
            output.format("Command:\n");
            output.format("\ttitle = \"%s\"\n", cObj.getData());
            output.format("\tParameters:\n");
            for (RoaeMd pObj : cObj.getChildren(RoaeMdType.PARAMETER)) {
                String description = pObj.getData();
                output.format("\t\t%s - %s\n", pObj.getName(), description == null ? "" : description);
            }
            output.format("\tBody:\n");
            for (RoaeMd sObj : cObj.getChildren(RoaeMdType.SQL)) {
                for (String line : sObj.getData().split("\n")) {
                    output.format("\t\t%s\n", line);
                }
            }
            output.format("\n");
        }
        output.close();
    }
}
