package no.nr.dbspec;

import org.treesitter.TSNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
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
        return obj == null ? "null" : shortName(obj.getClass());
    }

    public static void ensureInstance(TSNode n, String errorMessageStart, Object obj, Class<?>... classes) {
        for (Class<?> cls : classes) {
            if (cls.isInstance(obj)) {
                return;
            }
        }
        String t = Arrays.stream(classes).map(Utils::shortName).collect(Collectors.joining(" or "));
        throw new SemanticError(n, String.format("%s must be a%s %s, not %s.",
                errorMessageStart,
                t.matches("[aeiouyAEIOUY].*") ? "n" : "",
                t,
                getType(obj)));
    }

    public static String shortName(Class<?> cls) {
        if (cls == String.class) return "string";
        if (cls == BigInteger.class) return "integer";
        return cls.getSimpleName();
    }
}
