package no.nr.dbspec;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class RunInterpreter {
    public static void main(String[] args) {
        Option vOpt = new Option(
                "v", "verbosity", true,
                "verbosity level:\n0=fatal, 1=error, 2=warning, 3=info, 4=debug");
        Option dOpt = new Option("d", "directory", true, "working/root directory");
        Options options = new Options()
                .addOption(vOpt)
                .addOption(dOpt);

        CommandLine cmd;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("java -jar dbspec.jar OPTIONS FILENAME", options);
            System.exit(3);
            return; // Otherwise, cmd would have to be initialized.
        }
        Log log = new Log(Log.INFO); // Default log level

        String verbosityLevelString = cmd.getOptionValue(vOpt);
        if (verbosityLevelString != null) {
            log.setLevel(Integer.parseInt(verbosityLevelString));
        }

        String dirString = cmd.getOptionValue(dOpt);
        Path dir = Path.of(dirString == null ? System.getProperty("user.dir") : dirString);
        if (!Files.isDirectory(dir)) {
            log.write(Log.WARNING, "Error: The directory does not exist: " + dir);
            System.exit(4);
        }
        if (cmd.getArgs().length == 0) {
            log.write(Log.WARNING, "Error: missing input filename");
            System.exit(2);
        }
        Path file = dir.resolve(cmd.getArgs()[0]);
        if (!Files.exists(file)) {
            log.write(Log.WARNING, "Error: missing input filename");
            System.exit(2);
        }

        Properties config;
        try {
             config = loadConfigFile(dir);
        } catch (IOException e) {
            log.write(Log.WARNING, "Unable to read configuration file '%s'\n", CONFIG_FILENAME);
            config = new Properties();
        }

        Dbms dbms = new Dbms();
        Interpreter i = new Interpreter(log,
                dir,
                new ScriptRunnerImpl(),
                config,
                new SiardExtractorImpl(dbms, log, dir),
                dbms);
        if (!i.interpret(file)) {
            System.exit(1);
        }
    }

    public static final String CONFIG_FILENAME = "dbspec.conf";

    public static Properties loadConfigFile(Path dir) throws IOException {
        Properties config = new Properties();
        config.load(new FileInputStream(dir.resolve(CONFIG_FILENAME).toFile()));
        return config;
    }

}
