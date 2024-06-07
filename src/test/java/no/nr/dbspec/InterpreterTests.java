package no.nr.dbspec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class InterpreterTests {

    private static final Pattern statusCodePattern;
    private static final PathMatcher dbspecMatcher;
    private static final Path dir;
    private static final Properties properties;
    private static final Database database;
    private static final ScriptRunnerFake scriptRunner;
    private static final SiardExtractorFake extractor;

    private static final SiardMetadataAdjusterFake adjuster;

    static {
        statusCodePattern = Pattern.compile("^# Expected exit status code: ([0-9]+)$");
        dbspecMatcher = FileSystems.getDefault().getPathMatcher("glob:*.dbspec");

        try {
            // This is mainly a trick to find the directory containing the test .dbspec files.
            // Our tests will not work if these resources exist inside a .jar.
            URL confUrl = InterpreterTests.class.getResource("/" + RunInterpreter.CONFIG_FILENAME);
            assert confUrl != null;
            URI confUri = confUrl.toURI();
            assert "file".equals(confUri.getScheme());
            dir = Path.of(confUri).getParent();
            properties = RunInterpreter.loadConfigFile(dir);
            // We wanted to do this before and after each test,
            // but H2 still does not reliably give us a fresh (empty) database.
            database = new Database(
                    properties.getProperty("url"),
                    properties.getProperty("user"),
                    properties.getProperty("password"));
            scriptRunner = new ScriptRunnerFake(database);
            extractor = new SiardExtractorFake(database);
            adjuster = new SiardMetadataAdjusterFake(database);
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
                .map(Path::toString);
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

    private final Log log = new Log(Log.INFO);

    @ParameterizedTest
    @MethodSource
    void test_dbspec_files(String filename) throws Exception {
        Path path = dir.resolve(filename);
        File file = path.toFile();

        String line1 = new BufferedReader(new FileReader(file)).readLine();
        Matcher m = statusCodePattern.matcher(line1);
        assertTrue(m.matches());
        int expectedStatus = Integer.parseInt(m.toMatchResult().group(1));

        Interpreter i = new Interpreter(
                log,
                dir,
                scriptRunner,
                properties,
                extractor,
                new Dbms(),
                adjuster);
        boolean res = i.interpret(path);
        // TODO: Differentiate
        int exitStatusCode = res ? 0 : 1;
        assertEquals(expectedStatus, exitStatusCode);
    }
}
