package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.AttributeKey;

public class FilterAttributeNumeric {
    private AttributeKey key;
    private NumericOperator operator;
    private Double value;

    public FilterAttributeNumeric(AttributeKey key, NumericOperator operator, Double value) {
        this.key = key;
        this.operator = operator;
        this.value = value;
    }

    public AttributeKey getKey() {
        return key;
    }

    public NumericOperator getOperator() {
        return operator;
    }

    public Double getValue() {
        return value;
    }
}
