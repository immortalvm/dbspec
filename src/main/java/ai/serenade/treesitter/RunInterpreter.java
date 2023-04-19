package ai.serenade.treesitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class RunInterpreter {

	public static void main(String args[]) {
		if (args.length > 0) {
			String dbspec = getDbSpecString(args[0]);
			Interpreter i = new Interpreter(dbspec);
			i.interpret();
		} else {
			System.out.format("Error: missing input filename\n");
		}
	}

	static String getDbSpecString(String filename) {
		try {
			File file = new File(filename);
			Scanner s = new Scanner(file).useDelimiter("\\Z");
			String result = s.next();
			s.close();
			return result;
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			return null;
		}
	}

}
