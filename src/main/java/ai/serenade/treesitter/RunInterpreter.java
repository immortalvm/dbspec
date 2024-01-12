package ai.serenade.treesitter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class RunInterpreter {

	public static void main(String args[]) {
        Options options = new Options();
        Option verbosityOption = new Option("v", "verbosity", true, "verbosity level:\n0=fatal, 1=error, 2=warning, 3=info, 4=debug");
        options.addOption(verbosityOption);

        CommandLine cmd = null;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("java -jar dbspec.jar OPTIONS FILENAME", options);
            System.exit(3);
        }
        String verbosityLevelString = cmd.getOptionValue("verbosity");
        int verbosityLevel = verbosityLevelString == null ? Log.INFO : Integer.parseInt(verbosityLevelString);
        if (cmd.getArgs().length <= 0) {
        	System.out.format("Error: missing input filename\n");
            System.exit(2);
        }
        Interpreter i = new Interpreter(cmd.getArgs()[0], verbosityLevel);
        if (!i.interpret(verbosityLevel)) {
            System.exit(1);
        }
	}
}
