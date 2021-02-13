package info.mikaelsvensson.babyname.service.repository.names.request;

import java.util.Collection;
import java.util.LinkedList;

public class MetricsNameFacet extends DefaultNameFacet {
    public Collection<FilterAttributeNumeric> syllableFilter = new LinkedList<>();

    public MetricsNameFacet() {
        this.returned(true);
    }

    public MetricsNameFacet syllableFilter(FilterAttributeNumeric syllableFilter) {
        this.syllableFilter.add(syllableFilter);
        return this;
    }
}
