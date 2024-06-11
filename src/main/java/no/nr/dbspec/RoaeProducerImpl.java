package no.nr.dbspec;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import no.nr.dbspec.RoaeMd.RoaeMdType;

public class RoaeProducerImpl implements RoaeProducer {
    @Override
    public void updateMetadata(String roaeFilename, List<RoaeMd> commands, Log log, Path dir) throws IOException {
        Path path = dir.resolve(roaeFilename);
        if (commands == null || commands.isEmpty()) {
            String message = "No commands defined. ";
            if (Files.exists(path)) {
                Files.delete(path);
                message += "Deleting existing ROAE file: " + path;
            } else {
                message += "No ROAE file created.";
            }
            // TODO: Indent
            log.write(Log.INFO, message + "\n");
            return;
        }
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
