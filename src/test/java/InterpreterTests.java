import no.nr.dbspec.Interpreter;
import no.nr.dbspec.Log;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class InterpreterTests {

    private static final Pattern statusCodePattern;
    private static final PathMatcher dbspecMatcher;
    private static final Path confPath;

    static {
        statusCodePattern = Pattern.compile("^# Expected exit status code: ([0-9]+)$");
        dbspecMatcher = FileSystems.getDefault().getPathMatcher("glob:*.dbspec");

        try {
            // This is mainly a trick to find the directory containing the test .dbspec files.
            // Our tests will not work if these resources exist inside a .jar.
            URL confUrl = InterpreterTests.class.getResource("dbspec.conf");
            assertNotNull(confUrl);
            URI confUri = confUrl.toURI();
            assert "file".equals(confUri.getScheme());
            confPath = Path.of(confUri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<String> test_dbspec_files() throws IOException {
        // Wrapping this in a try-with-resource should be unnecessary.
        //noinspection resource
        return Files
                .list(confPath.getParent())
                .map(Path::getFileName)
                .filter(dbspecMatcher::matches)
                .map(Path::toString);
    }

    Log log = new Log(Log.INFO);

    @ParameterizedTest
    @MethodSource
    void test_dbspec_files(String filename) throws Exception {
        Path dir = confPath.getParent();
        Path path = dir.resolve(filename);
        File file = path.toFile();

        String line1 = new BufferedReader(new FileReader(file)).readLine();
        Matcher m = statusCodePattern.matcher(line1);
        assertTrue(m.matches());
        int expectedStatus = Integer.parseInt(m.toMatchResult().group(1));

        Interpreter i = new Interpreter(log, dir);
        boolean res = i.interpret(path);
        // TODO: Differentiate
        int exitStatusCode = res ? 0 : 1;
        assertEquals(expectedStatus, exitStatusCode);
    }
}
