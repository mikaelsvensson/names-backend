package info.mikaelsvensson.babyname.service.model.name;

import java.util.Objects;

public class Name {
    private String id;
    protected String name;
    private MetricsProperties metrics;
    private ScbProperties scb;
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

    public void setScb(ScbProperties scb) {
        this.scb = scb;
    }

    public ScbProperties getScb() {
        return scb;
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
}
