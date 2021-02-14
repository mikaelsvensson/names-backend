package info.mikaelsvensson.babyname.service.util.auth;

public interface UserAuthenticator {
    String getId(String token) throws UserAuthenticatorException;
}
