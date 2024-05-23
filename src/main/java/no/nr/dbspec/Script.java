package no.nr.dbspec;

import org.treesitter.TSNode;

import java.io.*;
import java.lang.InterruptedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Script {
    final static String EXEC_MARKER = "#!";

    // From https://stackoverflow.com/a/21541615
    private static final boolean isPosix =
            FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    public static String execute(TSNode n, String interpreter, String script) {
        String result;
        try {
            // Create script and make it executable
            File tempFile = File.createTempFile("script-", "");
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            if (isPosix) {
                bos.write(EXEC_MARKER.getBytes());
                bos.write(interpreter.getBytes());
                bos.write(10); // LF
            }
            bos.write(script.getBytes());
            bos.close();
            fos.close();
            if (isPosix) {
                Files.setPosixFilePermissions(tempFile.toPath(), PosixFilePermissions.fromString("rwx------"));
            }
            List<String> cmd = new LinkedList<>();
            if (!isPosix) {
                cmd.add(interpreter);
            }
            cmd.add(tempFile.getAbsolutePath());

            // Execute script and collect output
            Process process = new ProcessBuilder(cmd).start();
            process.waitFor();
            if (process.exitValue() != 0) {
                result = streamToString(process.getErrorStream());
                throw new ScriptError(n, ("Exit value: " + process.exitValue() + "\n" + result).trim());
            }
            result = streamToString(process.getInputStream());
        } catch (IOException e) {
            throw new ScriptError(n, e.getMessage());
        } catch (InterruptedException e) {
            throw new ScriptError(n, "Interrupted");
        }
        return result;
    }

    public static String streamToString(InputStream es) throws IOException {
        return new BufferedReader(new InputStreamReader(es))
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
