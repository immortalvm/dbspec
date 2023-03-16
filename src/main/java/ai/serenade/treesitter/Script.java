package ai.serenade.treesitter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

public class Script {
	final static String EXEC_MARKER = "#!";

	public static String execute(String interpreter, String script) {
		String result = "";
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
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			int ch;
			while ((ch = isr.read()) >= 0) {
				result += Character.toString(ch);
			}
		} catch (IOException e) {
			return "<script error>";
		}
		return result;
	}
	
}
