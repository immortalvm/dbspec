package no.nr.dbspec;

import org.treesitter.TSNode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedList;
import java.util.List;

public class ScriptRunnerImpl implements ScriptRunner {
    final static String EXEC_MARKER = "#!";

    // From https://stackoverflow.com/a/21541615
    private static final boolean isPosix =
            FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    @Override
    public String execute(TSNode n, String interpreter, String script, Path dir) {
        String result;
        try {
            // Create script and make it executable
            File tempFile = File.createTempFile("script-", "");
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            if (ScriptRunnerImpl.isPosix) {
                bos.write(ScriptRunnerImpl.EXEC_MARKER.getBytes());
                bos.write(interpreter.getBytes());
                bos.write(10); // LF
            }
            bos.write(script.getBytes());
            bos.close();
            fos.close();
            if (ScriptRunnerImpl.isPosix) {
                Files.setPosixFilePermissions(tempFile.toPath(), PosixFilePermissions.fromString("rwx------"));
            }
            List<String> cmd = new LinkedList<>();
            if (!ScriptRunnerImpl.isPosix) {
                cmd.add(interpreter);
            }
            cmd.add(tempFile.getAbsolutePath());

            // Execute script and collect output
            Process process = new ProcessBuilder(cmd).directory(dir.toFile()).start();
            process.waitFor();
            if (process.exitValue() != 0) {
                result = new String(process.getErrorStream().readAllBytes());
                throw new ScriptError(n, ("Exit value: " + process.exitValue() + "\n" + result).trim());
            }
            result = new String(process.getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new ScriptError(n, e.getMessage());
        } catch (InterruptedException e) {
            throw new ScriptError(n, "Interrupted");
        }
        return result;
    }
}
