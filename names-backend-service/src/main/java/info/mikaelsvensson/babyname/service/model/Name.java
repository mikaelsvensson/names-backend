package info.mikaelsvensson.babyname.service.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Name {
    private String id;
    protected String name;
    private Set<Attribute<?>> attributes;

    public Name() {
    }

    public Name(String name, String id, Set<Attribute<?>> attributes) {
        this.id = id;
        this.name = name;
        this.attributes = attributes;
    }

    public Optional<Attribute<?>> getAttribute(AttributeKey key) {
        return attributes.stream().filter(attribute -> attribute.getKey().equals(key)).findFirst();
    }

    public void addAttribute(Attribute<?> attribute) {
        attributes.add(attribute);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Attribute<?>> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Name name = (Name) o;
        return Objects.equals(id, name.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
