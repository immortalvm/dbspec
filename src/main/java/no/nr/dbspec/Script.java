package no.nr.dbspec;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.InterruptedException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

public class Script {
    final static String EXEC_MARKER = "#!";

    public static String execute(Node n, String interpreter, String script) {
        String result;
        try {
            // Create script and make it executable
            File tempFile = File.createTempFile("script-", "");
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(EXEC_MARKER.getBytes());
            bos.write(interpreter.getBytes());
            bos.write(10); // LF
            bos.write(script.getBytes());
            bos.close();
            fos.close();
            Files.setPosixFilePermissions(tempFile.toPath(), PosixFilePermissions.fromString("rwx------"));
            // Execute script and collect output
            Process process = new ProcessBuilder(tempFile.getAbsolutePath()).start();
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
        InputStreamReader esr = new InputStreamReader(es);
        String result = "";
        int ch;
        while ((ch = esr.read()) >= 0) {
            result += Character.toString(ch);
        }
        return result;
    }
}
