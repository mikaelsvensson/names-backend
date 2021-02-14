package info.mikaelsvensson.babyname.service.repository.names.request;

import java.util.Collection;
import java.util.LinkedList;

public class ScbNameFacet extends DefaultNameFacet {
    public Collection<FilterNumeric> percentOfPopulationFilter = new LinkedList<>();
    public Collection<FilterNumeric> percentWomenFilter = new LinkedList<>();

    public ScbNameFacet() {
        returned(true);
    }

    public ScbNameFacet percentOfPopulationFilter(FilterNumeric percentOfPopulationFilter) {
        this.percentOfPopulationFilter.add(percentOfPopulationFilter);
        return this;
    }

    public ScbNameFacet percentWomenFilter(FilterNumeric percentWomenFilter) {
        this.percentWomenFilter.add(percentWomenFilter);
        return this;
    }
}
