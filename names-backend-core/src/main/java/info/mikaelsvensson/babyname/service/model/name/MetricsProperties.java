package info.mikaelsvensson.babyname.service.model.name;

import java.util.Collections;
import java.util.List;

public class MetricsProperties {
    private final Double syllableCount;
    private final List<String> ngrams;

    public MetricsProperties(Double syllableCount) {
        this(syllableCount, Collections.emptyList());
    }

    public MetricsProperties(Double syllableCount, List<String> ngrams) {
        this.syllableCount = syllableCount;
        this.ngrams = ngrams;
    }

    public Double getSyllableCount() {
        return syllableCount;
    }

    public List<String> getNgrams() {
        return ngrams;
    }
}
