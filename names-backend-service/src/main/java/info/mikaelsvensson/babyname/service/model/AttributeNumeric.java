package info.mikaelsvensson.babyname.service.model;

public class AttributeNumeric extends Attribute<Double> {
    private final Double value;

    public AttributeNumeric(AttributeKey key, Double value) {
        super(key);
        this.value = value;
    }

    @Override
    public String getDisplayValue() {
        return value.toString();
    }

    @Override
    public Double getValue() {
        return value;
    }
}
