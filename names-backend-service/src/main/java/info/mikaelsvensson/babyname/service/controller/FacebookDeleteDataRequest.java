package info.mikaelsvensson.babyname.service.controller;

public class FacebookDeleteDataRequest {
    public String signed_request;

    public FacebookDeleteDataRequest() {
    }

    public String getSigned_request() {
        return signed_request;
    }

    public FacebookDeleteDataRequest setSigned_request(String signed_request) {
        this.signed_request = signed_request;
        return this;
    }
}
