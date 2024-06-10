package no.nr.dbspec;

public class ParameterRef {
    public final String name;

    public ParameterRef(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
