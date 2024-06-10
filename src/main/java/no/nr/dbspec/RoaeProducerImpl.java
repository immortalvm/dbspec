package no.nr.dbspec;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import no.nr.dbspec.CommandMd.CommandMdType;

public class RoaeProducerImpl implements RoaeProducer {
    @Override
    public void updateMetadata(String roaeFilename, List<CommandMd> commands, Log log, Path dir) throws IOException {
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
        for (CommandMd cObj : commands) {
            output.write(cObj.getDocumentation());
            output.format("%s\n", cObj.getDocumentation());
            for (CommandMd pObj : cObj.getChildren(CommandMdType.PARAMETER)) {
                output.format("%s - %s\n", pObj.getName(), pObj.getDocumentation());
            }
            for (CommandMd sObj : cObj.getChildren(CommandMdType.SQL)) {
                output.format("%s\n", sObj.getDocumentation());
            }
        }
        output.close();
    }
}
