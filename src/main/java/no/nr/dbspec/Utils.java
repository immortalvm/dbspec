package no.nr.dbspec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Utils {

    public static String streamToString(InputStream es) throws IOException {
        return new BufferedReader(new InputStreamReader(es))
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
