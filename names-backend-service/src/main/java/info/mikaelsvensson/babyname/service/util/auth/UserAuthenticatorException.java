package info.mikaelsvensson.babyname.service.util.auth;

public class UserAuthenticatorException extends Exception {
    public UserAuthenticatorException(Exception e) {
        super(e);
    }

    public UserAuthenticatorException(String message) {
        super(message);
    }
}
