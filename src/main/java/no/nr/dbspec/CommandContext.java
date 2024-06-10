package no.nr.dbspec;

import java.util.Set;

public class CommandContext implements Context {

    private final Context parent;
    private final Set<String> parameters;

    public CommandContext(Context parent, Set<String> parameters) {
        this.parent = parent;
        this.parameters = parameters;
    }

    @Override
    public Object getValue(String name) {
        return parameters.contains(name) ? new ParameterRef(name) : parent.getValue(name);
    }
}
