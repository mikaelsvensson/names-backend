package info.mikaelsvensson.babyname.service.model;

import java.time.Instant;

public class User {
    private final String id;
    private final String systemName;
    private final Instant createdAt;

    public User(String id, String systemName, Instant createdAt) {
        this.id = id;
        this.systemName = systemName;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSystemName() {
        return systemName;
    }
}
