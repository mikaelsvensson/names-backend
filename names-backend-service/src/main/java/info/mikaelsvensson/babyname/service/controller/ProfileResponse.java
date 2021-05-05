package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.UserProvider;

public class ProfileResponse {
    public UserProvider provider;
    public boolean isPartnerConnected;
    public long createdAt;

    public ProfileResponse() {
    }

    public ProfileResponse(UserProvider provider, boolean isPartnerConnected, long createdAt) {
        this.provider = provider;
        this.isPartnerConnected = isPartnerConnected;
        this.createdAt = createdAt;
    }

    public UserProvider getProvider() {
        return provider;
    }

    public void setProvider(UserProvider provider) {
        this.provider = provider;
    }

    public boolean isPartnerConnected() {
        return isPartnerConnected;
    }

    public void setPartnerConnected(boolean partnerConnected) {
        isPartnerConnected = partnerConnected;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
