package ai.serenade.treesitter;

public class RunInterpreter {

	public static void main(String args[]) {
		if (args.length > 0) {
			Interpreter i = new Interpreter(args[0]);
			i.interpret();
		} else {
			System.out.format("Error: missing input filename\n");
		}
	}


}
