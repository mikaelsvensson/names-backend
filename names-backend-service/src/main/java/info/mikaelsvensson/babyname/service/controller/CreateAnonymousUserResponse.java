package info.mikaelsvensson.babyname.service.controller;

public class CreateAnonymousUserResponse {
    public String id;

    public CreateAnonymousUserResponse() {
    }

    public CreateAnonymousUserResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
