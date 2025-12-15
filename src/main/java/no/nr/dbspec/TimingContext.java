package no.nr.dbspec;

/**
 * Context for tracking timing information during execution.
 * Thread-safe for concurrent time tracking.
 */
public class TimingContext {
    private final boolean enabled;
    private final long startTimeNanos;
    private long shellTimeNanos = 0;
    private long sqlTimeNanos = 0;
    private long siardTimeNanos = 0;

    public TimingContext(boolean enabled) {
        this.enabled = enabled;
        this.startTimeNanos = enabled ? System.nanoTime() : 0;
    }

    /**
     * Record time spent executing a shell command.
     * @param nanos Time in nanoseconds
     */
    public synchronized void addShellTime(long nanos) {
        if (enabled) {
            shellTimeNanos += nanos;
        }
    }

    /**
     * Record time spent executing SQL.
     * @param nanos Time in nanoseconds
     */
    public synchronized void addSqlTime(long nanos) {
        if (enabled) {
            sqlTimeNanos += nanos;
        }
    }

    /**
     * Record time spent extracting SIARD files.
     * @param nanos Time in nanoseconds
     */
    public synchronized void addSiardTime(long nanos) {
        if (enabled) {
            siardTimeNanos += nanos;
        }
    }

    /**
     * Print timing report to stderr.
     */
    public void printReport() {
        if (!enabled) {
            return;
        }

        long totalTimeNanos = System.nanoTime() - startTimeNanos;
        double totalTimeSeconds = totalTimeNanos / 1_000_000_000.0;
        double shellTimeSeconds = shellTimeNanos / 1_000_000_000.0;
        double sqlTimeSeconds = sqlTimeNanos / 1_000_000_000.0;
        double siardTimeSeconds = siardTimeNanos / 1_000_000_000.0;

        double shellPercentage = totalTimeNanos > 0 ? (shellTimeNanos * 100.0 / totalTimeNanos) : 0;
        double sqlPercentage = totalTimeNanos > 0 ? (sqlTimeNanos * 100.0 / totalTimeNanos) : 0;
        double siardPercentage = totalTimeNanos > 0 ? (siardTimeNanos * 100.0 / totalTimeNanos) : 0;

        System.err.println();
        System.err.println("Timing Report:");
        System.err.printf("  Total time:         %.3f s%n", totalTimeSeconds);
        System.err.printf("  Shell command time: %.3f s (%.1f%%)%n", shellTimeSeconds, shellPercentage);
        System.err.printf("  SQL command time:   %.3f s (%.1f%%)%n", sqlTimeSeconds, sqlPercentage);
        System.err.printf("  SIARD extract time: %.3f s (%.1f%%)%n", siardTimeSeconds, siardPercentage);
    }
}
