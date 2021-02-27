package info.mikaelsvensson.babyname.service.model.name;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PopulationProperties that = (PopulationProperties) o;
        return Objects.equals(percentOfPopulation, that.percentOfPopulation) &&
                Objects.equals(percentWomen, that.percentWomen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(percentOfPopulation, percentWomen);
    }
}
