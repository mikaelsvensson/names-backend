package info.mikaelsvensson.babyname.service.repository.names.request;

import info.mikaelsvensson.babyname.service.repository.names.NumericOperator;

public class FilterAttributeNumeric {
    private NumericOperator operator;
    private Double value;

    public FilterAttributeNumeric(NumericOperator operator, Double value) {
        this.operator = operator;
        this.value = value;
    }

    public NumericOperator getOperator() {
        return operator;
    }

    public Double getValue() {
        return value;
    }
}
