package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.Action;

public class VerifyEmailResult extends Action {
    public String redirectTo;
    public String token;

    public VerifyEmailResult(Action source, String token, String redirectTo) {
        super(source.getId(), source.getType(), source.getCreatedBy(), source.getStatus(), source.getParameters());
        this.token = token;
        this.redirectTo = redirectTo;
    }
}
