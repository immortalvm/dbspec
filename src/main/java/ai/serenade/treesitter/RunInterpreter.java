package ai.serenade.treesitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class RunInterpreter {
	final static String INPUT_FILENAME = "/home/thor/proj/idapiql/src/iDA-DbSpec/tree-sitter-dbspec/examples/complete_example.dbspec";

	public static void main(String args[]) {
			String dbspec = getDbSpecString(INPUT_FILENAME);
			System.out.println("--- Input ---");
			System.out.println(dbspec);
			System.out.println("-------------");
			Interpreter i = new Interpreter(dbspec);
			i.interpret();
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
