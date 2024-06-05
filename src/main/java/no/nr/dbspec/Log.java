package no.nr.dbspec;

import java.util.function.Supplier;

public class Log {
    public static final int FATAL = 0;
    public static final int ERROR = 1;
    public static final int WARNING = 2;
    public static final int INFO = 3;
    public static final int DEBUG = 4;

    int level;

    public Log(int level) {
        this.level = level;
    }

    void setLevel(int level) {
        this.level = level;
    }

    int getLevel() {
        return level;
    }

    public void write(int level, String message, Object... args) {
        if (level <= this.level) {
            System.out.format(message, args);
        }
    }

    public void write(int level, String message, Supplier<Object> arg) {
        if (level <= this.level) {
            System.out.format(message, arg.get());
        }
    }

    public void maybePrintStackTrace(Throwable e) {
        if (level >= Log.DEBUG) {
            printStackTrace(e);
        }
    }

    public void printStackTrace(Throwable e) {
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
    }
}
