package info.mikaelsvensson.babyname.service.controller;

public class AuthTokenResponse {
    private String token;

    public AuthTokenResponse(String token) {
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
