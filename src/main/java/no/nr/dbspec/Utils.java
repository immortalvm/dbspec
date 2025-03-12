package no.nr.dbspec;

import org.treesitter.TSNode;

import java.math.BigInteger;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public static String getCurrentTimeString() {
        return "[" + LocalTime.now().format(timeFormatter) + "] ";
    }

    // Cf. String.lines() and newline below.
    public static String stripFinalNewline(String str) {
        // There might be more elegant ways to do this...
        if (str.isEmpty()) {
            return str;
        }
        int n = str.length();
        return str.charAt(n - 1) == '\r' ? str.substring(0, n - 1)
                : str.charAt(n - 1) != '\n' ? str
                : n < 2 || str.charAt(n - 2) != '\r' ? str.substring(0, n - 1)
                : str.substring(0, n - 2);
    }

    public static String prefixAndFixLineSeparators(String prefix, String str) {
        return (str + "\r\n").lines()
                .map(x -> prefix + x)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    // Unlike String.lines(), a single \r does not end the line.
    private static final Pattern tsNewline = Pattern.compile("\r?\n");

    public static String[] tsLines(String str) {
        // -1: Include trailing empty strings, also after final newline/CR+LF.
        return tsNewline.split(str, -1);
    }

    public static String[] tsLineEndings(String str) {
        return tsNewline.matcher(str).results().map(MatchResult::group).toArray(String[]::new);
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
        } else if (obj instanceof Rows) {
            try {
                return "[" + ((Rows) obj).getSize() + " rows]";
            } catch (SQLException e) {
                return "[?]";
            }
        }
        return obj == null ? "null" : obj.toString();
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
        throw new SemanticFailure(n, String.format("%s must be a%s %s, not %s.",
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
