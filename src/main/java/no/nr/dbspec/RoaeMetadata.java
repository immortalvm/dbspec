package no.nr.dbspec;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import no.nr.dbspec.MdObject.MdType;

public class RoaeMetadata {
    public static void updateMetadata(String roaeFilename, MdObject mdo, Log log, Path dir) throws IOException {
        Path path = dir.resolve(roaeFilename);
        List<MdObject> commands = mdo.getChildren(MdType.COMMAND);
        if (commands.isEmpty()) {
            String message = "No commands defined. ";
            if (Files.exists(path)) {
                Files.delete(path);
                message += "Deleting existing ROAE file: " + path;
            } else {
                message += "No ROAE file created.";
            }
            // TODO: Indent
            log.write(Log.INFO, message);
        }
        PrintWriter output = new PrintWriter(new FileWriter(path.toFile()));
        for (MdObject cObj : commands) {
            output.write(cObj.getDocumentation());
            output.format("%s\n", cObj.getDocumentation());
            for (MdObject pObj : cObj.getChildren(MdType.PARAMETER)) {
                output.format("%s - %s\n", pObj.getName(), pObj.getDocumentation());
            }
            for (MdObject sObj : cObj.getChildren(MdType.SQL)) {
                output.format("%s\n", sObj.getDocumentation());
            }
        }
        output.close();
    }
}
