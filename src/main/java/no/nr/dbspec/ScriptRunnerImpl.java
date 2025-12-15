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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ScriptRunnerImpl implements ScriptRunner {
    private static final String EXEC_MARKER = "#!";
    private final TimingContext timingContext;

    // From https://stackoverflow.com/a/21541615
    private static final boolean isPosix =
            FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    public ScriptRunnerImpl(TimingContext timingContext) {
        this.timingContext = timingContext;
    }

    /**
     * NB. Strips (at most one) final \r?\n from the output.
     */
    @Override
    public String execute(TSNode n, String interpreter, String script, Path dir) {
        long startTime = System.nanoTime();
        String result;
        try {
            // Create script and make it executable
            // The suffix .ps1 for non-posix systems is a hack since PowerShell only accepts this suffix.
            File tempFile = File.createTempFile("script-", isPosix ? "" : ".ps1");
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
                cmd.addAll(Arrays.asList(interpreter.split(" ")));
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
        } finally {
            if (timingContext != null) {
                timingContext.addShellTime(System.nanoTime() - startTime);
            }
        }
        }
        return Utils.stripFinalNewline(result);
    }
}
