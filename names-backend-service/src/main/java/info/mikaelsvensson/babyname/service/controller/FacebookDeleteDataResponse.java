package info.mikaelsvensson.babyname.service.controller;

public class FacebookDeleteDataResponse {
    public String url;
    public String confirmation_code;

    public FacebookDeleteDataResponse() {
    }

    public FacebookDeleteDataResponse(String url, String confirmation_code) {
        this.url = url;
        this.confirmation_code = confirmation_code;
    }

    public String getUrl() {
        return url;
    }

    public FacebookDeleteDataResponse setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getConfirmation_code() {
        return confirmation_code;
    }

    public FacebookDeleteDataResponse setConfirmation_code(String confirmation_code) {
        this.confirmation_code = confirmation_code;
        return this;
    }
}
