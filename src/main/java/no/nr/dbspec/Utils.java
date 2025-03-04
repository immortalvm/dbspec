package no.nr.dbspec;

import org.treesitter.TSNode;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    public static final Pattern newline = Pattern.compile("\r?\n");

    /**
     * The last line may or may not end with \r?\n,
     * i.e. at most one trailing empty line is skipped.
     * See the unit test for details.
     */
    public static String[] lines(String str) {
        // -1: Include trailing empty strings, also after final newline/CRLF.
        String[] a = newline.split(str, -1);
        int n = a.length;
        return n > 0 && a[n - 1].isEmpty() ? Arrays.copyOfRange(a, 0, n - 1) : a;
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
