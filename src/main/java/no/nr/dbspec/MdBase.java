package no.nr.dbspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MdBase<T extends Enum<T>, D extends MdBase<T, D>> {
    private final T type;
    private final String name;
    private final String data;
    private final List<D> children;

    /**
     * @param type Node type
     * @param name Node name
     * @param data Content other than children
     */
    protected MdBase(T type, String name, String data) {
        this.type = type;
        this.name = name;
        this.data = data;
        this.children = new ArrayList<>();
    }

    public T getType() {
        return type;
    }

    public String getName() {
        return this.name;
    }

    public String getData() {
        return this.data;
    }

    public void add(D child) {
        this.children.add(child);
    }

    public Stream<D> childStream() {
        return children.stream();
    }

    // Only for testing
    @Override
    public String toString() {
        return String.format(
                "(%s %s %s%s%s)",
                type, name, Utils.escape(data, false), children.isEmpty() ? "" : " ",
                childStream().map(MdBase::toString).collect(Collectors.joining(" ")));
    }

    public List<D> getChildren(T type) {
        return childStream().filter((x) -> x.getType() == type).collect(Collectors.toList());
    }

    public D getChild(T type, String name) {
        Optional<D> found = childStream()
                .filter(x -> x.getType() == type && x.getName().equals(name))
                .findAny();
        return found.orElse(null);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
