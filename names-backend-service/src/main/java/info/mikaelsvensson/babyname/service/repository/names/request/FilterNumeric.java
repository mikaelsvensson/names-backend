package info.mikaelsvensson.babyname.service.repository.names.request;

public class FilterNumeric {
    private FilterNumericOperator operator;
    private Double value;

    public FilterNumeric(FilterNumericOperator operator, Double value) {
        this.operator = operator;
        this.value = value;
    }

    public FilterNumericOperator getOperator() {
        return operator;
    }

    public Double getValue() {
        return value;
    }
}
