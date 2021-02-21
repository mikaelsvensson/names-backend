package info.mikaelsvensson.babyname.service.repository.names.request;

import java.util.Collection;
import java.util.LinkedList;

public class PopulationNameFacet extends DefaultNameFacet {
    public Collection<FilterNumeric> percentOfPopulationFilter = new LinkedList<>();
    public Collection<FilterNumeric> percentWomenFilter = new LinkedList<>();

    public PopulationNameFacet() {
        returned(true);
    }

    public PopulationNameFacet percentOfPopulationFilter(FilterNumeric percentOfPopulationFilter) {
        this.percentOfPopulationFilter.add(percentOfPopulationFilter);
        return this;
    }

    public PopulationNameFacet percentWomenFilter(FilterNumeric percentWomenFilter) {
        this.percentWomenFilter.add(percentWomenFilter);
        return this;
    }
}
