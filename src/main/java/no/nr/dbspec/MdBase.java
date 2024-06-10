package no.nr.dbspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class MdBase<T extends Enum<T>, D extends MdBase<T, D>> {
    protected final T type;
    protected final String name;
    protected final String documentation;
    protected final List<D> children;

    protected MdBase(T type, String name, String documentation) {
        this.type = type;
        this.name = name;
        this.documentation = documentation;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public String getDocumentation() {
        return this.documentation;
    }

    public void add(D child) {
        this.children.add(child);
    }

    // Only for testing
    @Override
    public String toString() {
        return String.format(
                "(%s %s %s%s%s)",
                type, name, Utils.escape(documentation, false), children.isEmpty() ? "" : " ",
                children.stream().map(MdBase::toString).collect(Collectors.joining(" ")));
    }

    public List<D> getChildren(T type) {
        return children.stream().filter((x) -> x.type == type).collect(Collectors.toList());
    }

    public D getChild(T type, String name) {
        Optional<D> found = children.stream().filter((x) -> x.type == type && x.name.equals(name)).findAny();
        return found.orElse(null);
    }

    public boolean hasNoChildren() {
        return children.isEmpty();
    }
}
