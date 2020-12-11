package info.mikaelsvensson.babyname.service.model;

import java.time.Instant;

public class User {
    private final String id;
    private final UserProvider providerKey;
    private final String providerUserId;
    private final Instant createdAt;

    public User(String id, Instant createdAt) {
        this(id, UserProvider.ANONYMOUS, null, createdAt);
    }

    public User(String id, UserProvider providerKey, String providerUserId, Instant createdAt) {
        this.id = id;
        this.providerKey = providerKey;
        this.providerUserId = providerUserId;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public UserProvider getProviderKey() {
        return providerKey;
    }
}
