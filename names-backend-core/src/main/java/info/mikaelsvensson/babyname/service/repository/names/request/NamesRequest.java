package info.mikaelsvensson.babyname.service.repository.names.request;

import info.mikaelsvensson.babyname.service.repository.names.Country;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

public class NamesRequest {
    public int offset = 0;
    public int limit = Integer.MAX_VALUE;
    public BasicNameFacet basic;
    public MetricsNameFacet metrics;
    public Map<Country, PopulationNameFacet> demographics;
    public VotesNameFacet votes;
    public SortOrder sortOrder;
    public String sortOrderParam;

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

    public NamesRequest demographics(PopulationNameFacet demographics, Country... countries) {
        if (this.demographics == null) {
            this.demographics = new HashMap<>();
        }
        for (Country country : countries) {
            this.demographics.put(country, demographics);
        }
        return this;
    }

    public NamesRequest votes(VotesNameFacet votes) {
        this.votes = votes;
        return this;
    }

    public NamesRequest sortOrder(SortOrder order, String param) {
        try {
            this.sortOrder = order;
            this.sortOrderParam = switch (order) {
                case NAME -> null;
                case RANDOM -> Long.toString(Long.parseLong(param));
            };
            return this;
        } catch (IllegalArgumentException e) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        }
    }
}
