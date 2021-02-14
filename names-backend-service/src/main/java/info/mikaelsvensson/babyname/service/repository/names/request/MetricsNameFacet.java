package info.mikaelsvensson.babyname.service.repository.names.request;

import java.util.Collection;
import java.util.LinkedList;

public class MetricsNameFacet extends DefaultNameFacet {
    public Collection<FilterNumeric> syllableFilter = new LinkedList<>();

    public MetricsNameFacet() {
        this.returned(true);
    }

    public MetricsNameFacet syllableFilter(FilterNumeric syllableFilter) {
        this.syllableFilter.add(syllableFilter);
        return this;
    }
}
