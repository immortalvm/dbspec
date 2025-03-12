package no.nr.dbspec;

import java.util.function.Supplier;

/**
 * Logging to standard error
 * NB. DbSpec Log statements write to standard output if level > QUIET.
 */
public class Log {
    public static final int QUIET = 0; // This even suppresses DbSpec Log statements !
    public static final int NORMAL = 1;
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;

    private final int level;

    public Log(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Log error.
     */
    public void error(String message, Object... args) {
        if (level > QUIET) {
            printToStderr("ERROR: ", message, args);
        }
    }

    public void verbose(String message, Object... args) {
        if (level >= VERBOSE) {
            printToStderr("", message, args);
        }
    }

    public void debug(String message, Object... args) {
        debugIndented(0, message, args);
    }

    // NB. This is the primary (only?) place we actually use 'indent'.
    public void debugIndented(int indent, String message, Object... args) {
        if (level >= DEBUG) {
            printToStderr("DEBUG: " + "  ".repeat(indent), message, args);
        }
    }

    private static void printToStderr(String prefix, String message, Object[] args) {
        unpack(args);
        System.err.println(
                Utils.prefixAndFixLineSeparators(
                        Utils.getCurrentTimeString() + prefix,
                        String.format(message, args)));
    }

    private static void unpack(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Supplier) {
                args[i] = ((Supplier<?>) args[i]).get();
            }
        }
    }

    public void maybePrintStackTrace(Throwable e) {
        if (level >= Log.DEBUG) {
            e.printStackTrace(System.err);
        }
    }

    public void printStackTrace(Throwable e) {
        if (level > Log.QUIET) {
            e.printStackTrace(System.err);
        }
    }

    public void newline() {
        if (level > QUIET) {
            System.err.println();
        }
    }
}
