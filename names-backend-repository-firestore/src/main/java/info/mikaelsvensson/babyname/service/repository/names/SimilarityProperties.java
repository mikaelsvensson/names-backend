package info.mikaelsvensson.babyname.service.repository.names;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class SimilarityProperties {
    private final List<String> nameIds;
    private final Instant createdAt;

    public SimilarityProperties() {
        this(Collections.emptyList(), Instant.ofEpochMilli(0));
    }

    public SimilarityProperties(List<String> nameIds, Instant createdAt) {
        this.nameIds = nameIds;
        this.createdAt = createdAt;
    }

    public List<String> getNameIds() {
        return nameIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
