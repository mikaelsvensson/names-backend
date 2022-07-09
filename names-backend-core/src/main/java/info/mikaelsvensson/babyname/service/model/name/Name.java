package info.mikaelsvensson.babyname.service.model.name;

import info.mikaelsvensson.babyname.service.repository.names.Country;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Name {
    private String id;
    protected String name;
    private MetricsProperties metrics;
    private Map<Country, PopulationProperties> demographics;
    private VotesProperties votes;
    private RecommendationProperties recommendation;

    public Name() {
    }

    public Name(String name, String id) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Name name = (Name) o;
        return Objects.equals(id, name.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void setMetrics(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    public MetricsProperties getMetrics() {
        return metrics;
    }

    public void setVotes(VotesProperties votes) {
        this.votes = votes;
    }

    public VotesProperties getVotes() {
        return votes;
    }

    public void setRecommendation(RecommendationProperties recommendation) {
        this.recommendation = recommendation;
    }

    public RecommendationProperties getRecommendation() {
        return recommendation;
    }

    public Map<Country, PopulationProperties> getDemographics() {
        return demographics;
    }

    public Name setDemographics(Map<Country, PopulationProperties> demographics) {
        this.demographics = demographics;
        return this;
    }

    public void putDemographics(Country country, PopulationProperties properties) {
        if (demographics == null) {
            demographics = new HashMap<>();
        }
        demographics.putIfAbsent(country, properties);
    }
}
