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

    public static String escape(Object o, boolean properly){
        if (o instanceof String) {
            String x = ((String) o)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            if (properly) {
                x = x
                        .replace("\t", "\\t")
                        .replace("\b", "\\b")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\f", "\\f");
            }
            return "\"" + x + "\"";
        }
        return o.toString();
    }
}
