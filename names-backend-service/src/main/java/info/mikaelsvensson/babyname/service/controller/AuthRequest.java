package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.UserProvider;

public class AuthRequest {
    public UserProvider provider;
    public String data;

    public AuthRequest() {
    }

    public AuthRequest(UserProvider provider, String data) {
        this.provider = provider;
        this.data = data;
    }
}
