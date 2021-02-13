package info.mikaelsvensson.babyname.service.repository.names.request;

import java.util.Collection;
import java.util.LinkedList;

public class ScbNameFacet extends DefaultNameFacet {
    public Collection<FilterAttributeNumeric> percentOfPopulationFilter = new LinkedList<>();
    public Collection<FilterAttributeNumeric> percentWomenFilter = new LinkedList<>();

    public ScbNameFacet() {
        returned(true);
    }

    public ScbNameFacet percentOfPopulationFilter(FilterAttributeNumeric percentOfPopulationFilter) {
        this.percentOfPopulationFilter.add(percentOfPopulationFilter);
        return this;
    }

    public ScbNameFacet percentWomenFilter(FilterAttributeNumeric percentWomenFilter) {
        this.percentWomenFilter.add(percentWomenFilter);
        return this;
    }
}
