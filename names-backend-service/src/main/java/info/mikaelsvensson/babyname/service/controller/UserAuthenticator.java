package info.mikaelsvensson.babyname.service.controller;

public interface UserAuthenticator {
    String getId(String token) throws UserAuthenticatorException;
}
