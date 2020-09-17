package info.mikaelsvensson.babyname.service.model;

public abstract class Attribute<V> {
    private final AttributeKey key;

    public Attribute(AttributeKey key) {
        this.key = key;
    }

    public AttributeKey getKey() {
        return key;
    }

    abstract public String getDisplayValue();

    abstract public V getValue();
}
