package no.nr.dbspec;

import org.treesitter.TSNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {

    public static String streamToString(InputStream es) throws IOException {
        return new BufferedReader(new InputStreamReader(es))
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public static String escape(Object obj, boolean properly){
        if (obj instanceof String) {
            String x = ((String) obj)
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
        return obj == null ? null : obj.toString();
    }

    public static String getType(Object obj) {
        return obj == null ? "null" : obj.getClass().getSimpleName();
    }

    public static void ensureInstance(TSNode n, String errorMessageStart, Object obj, Class<?>... classes) {
        for (Class<?> cls : classes) {
            if (cls.isInstance(obj)) {
                return;
            }
        }
        throw new SemanticError(n, String.format("%s must be a %s, not %s.",
                errorMessageStart,
                Arrays.stream(classes).map(Class::getSimpleName).collect(Collectors.joining(" or ")),
                getType(obj)));
    }
}
