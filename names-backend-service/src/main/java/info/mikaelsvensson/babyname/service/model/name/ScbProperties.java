package info.mikaelsvensson.babyname.service.model.name;

public class ScbProperties {
    private final Double percentOfPopulation;
    private final Double percentWomen;

    public ScbProperties(Double percentOfPopulation, Double percentWomen) {
        this.percentOfPopulation = percentOfPopulation;
        this.percentWomen = percentWomen;
    }

    public Double getPercentOfPopulation() {
        return percentOfPopulation;
    }

    public Double getPercentWomen() {
        return percentWomen;
    }
}
