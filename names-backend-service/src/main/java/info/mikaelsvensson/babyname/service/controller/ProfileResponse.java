package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.UserProvider;

public class ProfileResponse {
    public final UserProvider provider;
    public final boolean isPartnerConnected;
    public final long createdAt;

    public ProfileResponse(UserProvider provider, boolean isPartnerConnected, long createdAt) {
        this.provider = provider;
        this.isPartnerConnected = isPartnerConnected;
        this.createdAt = createdAt;
    }
}
