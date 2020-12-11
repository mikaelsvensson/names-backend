package info.mikaelsvensson.babyname.service.controller;

import java.security.NoSuchAlgorithmException;

public class UserAuthenticatorException extends Exception {
    public UserAuthenticatorException(Exception e) {
        super(e);
    }

    public UserAuthenticatorException(String message) {
        super(message);
    }
}
