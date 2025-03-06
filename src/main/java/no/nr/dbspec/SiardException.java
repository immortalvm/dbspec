package no.nr.dbspec;

public class SiardException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    String reason;

    public SiardException(Object reason) {
        this.reason = reason == null ? "" : reason.toString();
    }

    public String getReason() {
        return reason;
    }
}
