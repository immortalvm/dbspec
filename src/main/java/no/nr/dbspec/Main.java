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

public class Main {
    private static final Option quietOpt = new Option("q", "quiet", false,
            "Produce no output even when failing (not guaranteed in extreme cases)");
    private static final Option verboseOpt = new Option(
            "v", "verbose", false,
            "Turn on verbose logging (for even more verbose logging, use '-debug')");
    private static final Option debugOpt = new Option(
            null, "debug", false, "Turn on debug logging");
    private static final Option dirOpt = new Option(
            "d", "directory", true, "Set working/root directory");
    private static final Options options = new Options()
            .addOption(verboseOpt)
            .addOption(dirOpt);

    public static void main(String[] args) {
        System.exit(run(args).getValue());
    }

    private static StatusCode run(String[] args) {
        CommandLine cmd;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("java -jar dbspec.jar OPTIONS FILENAME", options);
            return StatusCode.COULD_NOT_PARSE_OPTIONS;
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
            return StatusCode.DIRECTORY_DOES_NOT_EXIST;
        }
        if (cmd.getArgs().length == 0) {
            log.error("Missing input filename.");
            return StatusCode.DBSPEC_FILE_NOT_SPECIFIED;
        }
        Path file = dir.resolve(cmd.getArgs()[0]);
        if (!Files.exists(file)) {
            log.error("File not found.");
            return StatusCode.DBSPEC_FILE_NOT_FOUND;
        }

        Path configPath = dir.resolve(CONFIG_FILENAME);
        Properties config;
        try {
            if (Files.exists(configPath)) {
                config = loadConfigFile(configPath);
            } else {
                log.verbose("Configuration file not found: %s", configPath);
                config = new Properties();
            }
        } catch (IOException e) {
            log.error("Unable to read configuration file: %s\n%s", configPath, e.getMessage());
            return StatusCode.CONFIG_FILE_UNREADABLE;
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

        return i.interpret(file);
    }

    public static final String CONFIG_FILENAME = "dbspec.conf";

    public static Properties loadConfigFile(Path configPath) throws IOException {
        Properties config = new Properties();
        config.load(new FileInputStream(configPath.toFile()));
        return config;
    }
}
