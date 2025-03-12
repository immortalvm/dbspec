package no.nr.dbspec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class InterpreterTests {

    private static final Pattern statusCodePattern = Pattern.compile("^# Expected exit status code: ([A-Z_]+)$");
    private static final PathMatcher dbspecMatcher = FileSystems.getDefault().getPathMatcher("glob:*.dbspec");
    private static final String logTestFilename = "Log.dbspec";
    private static final Set<String> handledSeparately = Set.of(logTestFilename);

    private static final Path dir;
    private static final Properties properties;
    private static final Database database;
    private static final ScriptRunnerFake scriptRunner;
    private static final SiardExtractorFake extractor;
    private static final SiardMetadataAdjusterFake adjuster;
    private static final RoaeProducer roaeProducer;

    static {
        try {
            // This is mainly a trick to find the directory containing the test .dbspec files.
            // Our tests will not work if these resources exist inside a .jar.
            URL confUrl = InterpreterTests.class.getResource("/" + Main.DEFAULT_CONFIG_FILENAME);
            assert confUrl != null;
            URI confUri = confUrl.toURI();
            assert "file".equals(confUri.getScheme());
            Path confPath = Path.of(confUri);
            dir = confPath.getParent();
            properties = Main.loadConfigFile(confPath);
            // We wanted to do this before and after each test,
            // but H2 still does not reliably give us a fresh (empty) database.
            database = new Database(
                    properties.getProperty("url"),
                    properties.getProperty("user"),
                    properties.getProperty("password"));
            scriptRunner = new ScriptRunnerFake(database);
            extractor = new SiardExtractorFake(database);
            adjuster = new SiardMetadataAdjusterFake(database);
            roaeProducer = new RoaeProducerFake(database);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<String> test_dbspec_files() throws IOException {
        // Wrapping this in a try-with-resource should be unnecessary.
        //noinspection resource
        return Files
                .list(dir)
                .map(Path::getFileName)
                .filter(dbspecMatcher::matches)
                .map(Path::toString)
                .filter(name -> !handledSeparately.contains(name));
    }

    @AfterEach
    void reset() throws SQLException {
        // NB. Only the "trace" table is actually reset.
        database.resetTrace();
    }

    @AfterAll
    static void closeDatabase() throws Exception {
        database.close();
    }

    private final Log log = new Log(Log.NORMAL);

    @ParameterizedTest
    @MethodSource
    void test_dbspec_files(String filename) throws Exception {
        Path path = dir.resolve(filename);
        File file = path.toFile();

        String line1 = new BufferedReader(new FileReader(file)).readLine();
        Matcher m = statusCodePattern.matcher(line1);
        assertTrue(m.matches(), "Missing expected exit status");
        StatusCode expectedStatus = StatusCode.valueOf(m.toMatchResult().group(1));

        Interpreter i = new Interpreter(
                log,
                dir,
                scriptRunner,
                properties,
                extractor,
                new Dbms(),
                adjuster,
                roaeProducer);
        StatusCode code = i.interpret(path);
        assertEquals(expectedStatus, code);
    }

    @Test
    void test_Log_statements() throws Exception {
        PrintStream originalOut = System.out;
        try {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent, true));
            test_dbspec_files(logTestFilename);
            String[] lines = outContent.toString().lines().toArray(String[]::new);
            int i = 0;
            assertEquals("This is logged", lines[i++]);
            assertEquals("17", lines[i++]);
            assertEquals("Multiline log message", lines[i++]);
            assertEquals("\twith tabs and interpolation: 9", lines[i++]);
            assertEquals(i, lines.length);
        } finally {
            System.setOut(originalOut);
        }
    }
}
