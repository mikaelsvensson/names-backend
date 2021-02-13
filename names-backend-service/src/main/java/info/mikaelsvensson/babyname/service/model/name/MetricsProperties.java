package info.mikaelsvensson.babyname.service.model.name;

public class MetricsProperties {
    private final Double syllableCount;

    public MetricsProperties(Double syllableCount) {
        this.syllableCount = syllableCount;
    }

    public Double getSyllableCount() {
        return syllableCount;
    }
}
