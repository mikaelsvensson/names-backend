package info.mikaelsvensson.babyname.service.model.name;

public class PopulationProperties {
    private final Double percentOfPopulation;
    private final Double percentWomen;

    public PopulationProperties(Double percentOfPopulation, Double percentWomen) {
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
