package info.mikaelsvensson.babyname.service.model;

import info.mikaelsvensson.babyname.service.util.IdUtils;

import java.time.OffsetDateTime;

public class User {
    private String id;
    private OffsetDateTime createdAt;

    public User() {
        id = IdUtils.random();
        createdAt = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
