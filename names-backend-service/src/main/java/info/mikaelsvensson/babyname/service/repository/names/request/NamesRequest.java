package info.mikaelsvensson.babyname.service.repository.names.request;

import info.mikaelsvensson.babyname.service.repository.names.Country;

import java.util.HashMap;
import java.util.Map;

public class NamesRequest {
    public int offset = 0;
    public int limit = Integer.MAX_VALUE;
    public BasicNameFacet basic;
    public MetricsNameFacet metrics;
    public Map<Country, PopulationNameFacet> demographics;
    public VotesNameFacet votes;

    public NamesRequest offset(int offset) {
        this.offset = offset;
        return this;
    }

    public NamesRequest limit(int limit) {
        this.limit = limit;
        return this;
    }

    public NamesRequest basic(BasicNameFacet basic) {
        this.basic = basic;
        return this;
    }

    public NamesRequest metrics(MetricsNameFacet metrics) {
        this.metrics = metrics;
        return this;
    }

    public NamesRequest demographics(Country country, PopulationNameFacet demographics) {
        if (this.demographics == null) {
            this.demographics = new HashMap<>();
        }
        this.demographics.put(country, demographics);
        return this;
    }

    public NamesRequest votes(VotesNameFacet votes) {
        this.votes = votes;
        return this;
    }
}
