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
        Option quietOpt = new Option("q", "quiet", false,
                "Produce no output even when failing (not guaranteed in extreme cases)");
        Option verboseOpt = new Option(
                "v", "verbose", false,
                "Turn on verbose logging (for even more verbose logging, use '-debug')");
        Option debugOpt = new Option(
                null, "debug", false, "Turn on debug logging");
        Option dirOpt = new Option(
                "d", "directory", true, "Set working/root directory");
        Options options = new Options()
                .addOption(verboseOpt)
                .addOption(dirOpt);

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
        int logLevel = cmd.hasOption(quietOpt) ? Log.QUIET
                : cmd.hasOption(debugOpt) ? Log.DEBUG
                : cmd.hasOption(verboseOpt) ? Log.VERBOSE
                : Log.NORMAL;
        Log log = new Log(logLevel); // Default log level

        String dirString = cmd.getOptionValue(dirOpt);
        Path dir = Path.of(dirString == null ? System.getProperty("user.dir") : dirString);
        if (!Files.isDirectory(dir)) {
            log.error("The directory does not exist: " + dir);
            System.exit(4);
        }
        if (cmd.getArgs().length == 0) {
            log.error("Missing input filename.");
            System.exit(2);
        }
        Path file = dir.resolve(cmd.getArgs()[0]);
        if (!Files.exists(file)) {
            log.error("File not found.");
            System.exit(2);
        }

        Properties config;
        try {
             config = loadConfigFile(dir);
        } catch (IOException e) {
            log.error("Unable to read configuration file '%s'.", CONFIG_FILENAME);
            config = new Properties();
        }

        Dbms dbms = new Dbms();
        Interpreter i = new Interpreter(log,
                dir,
                new ScriptRunnerImpl(),
                config,
                new SiardExtractorImpl(dbms, log, dir),
                dbms,
                new SiardMetadataAdjusterImpl(),
                new RoaeProducerImpl());
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
