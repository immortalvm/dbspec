package no.nr.dbspec;

import org.treesitter.TSNode;

import java.math.BigInteger;
import java.sql.Connection;
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
                Rows rows = (Rows) obj;
                int n = rows.getSize();
                String[] row;
                String contents = n > 0
                            && rows.tryLockAndRewind()
                            && 0 < (row = rows.next()).length
                        ? Arrays.stream(row)
                            .map(x -> escape(x, properly))
                            .collect(Collectors.joining(", "))
                        : "";
                return String.format("[%d row%s%s%s%s%s]",
                        n,
                        pluralS(n),
                        contents.isEmpty() ? "" : ": (",
                        contents,
                        contents.isEmpty() ? "" : ")",
                        n > 1 ? ", ..." : "");
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
        throw new SemanticFailure(n, String.format("%s must be %s, not %s.",
                errorMessageStart,
                indefinite(t),
                indefinite(getType(obj))));
    }

    public static String shortName(Class<?> cls) {
        return cls == String.class ? "string"
                : cls == BigInteger.class ? "integer"
                : Rows.class.isAssignableFrom(cls) ? "list of rows"
                : Connection.class.isAssignableFrom(cls) ? "database connection"
                : cls.getSimpleName();
    }

    public static String indefinite(String noun) {
        return (noun.matches("[aeiouyAEIOUY].*") ? "an " : "a ") + noun;
    }

    public static String pluralS(int n) {
        return n == 1 ? "" : "s";
    }
}
