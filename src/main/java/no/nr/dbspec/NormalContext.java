package no.nr.dbspec;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.Map;
import java.util.stream.Collectors;

public class NormalContext implements Context {
    Map<String,Object> bindings;

    public NormalContext() {
        this.bindings = new HashMap<>();
    }

    public void setValue(String name, Object value) {
        bindings.put(name, value);
    }

    public void clearValue(String name) {
        bindings.remove(name);
    }

    @Override
    public Object getValue(String name) {
        return bindings.get(name);
    }

    public String toString() {
        return bindings.entrySet().stream()
                .map(e -> String.format("%s='%s'", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public void forEach(BiConsumer<String, Object> action) {
        bindings.forEach((k, v) -> action.accept(k, getValue(k)));
    }
}
