package info.mikaelsvensson.babyname.service.model.name;

public class RecommendationProperties {
    private final double score;

    public RecommendationProperties(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }
}
