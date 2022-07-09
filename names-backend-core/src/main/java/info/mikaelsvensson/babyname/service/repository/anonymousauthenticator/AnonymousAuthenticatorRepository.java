package info.mikaelsvensson.babyname.service.repository.anonymousauthenticator;

import org.springframework.stereotype.Repository;

@Repository
public interface AnonymousAuthenticatorRepository {
    boolean exists(String id) throws AnonymousAuthenticatorException;

    void logUse(String id) throws AnonymousAuthenticatorException;

    String create() throws AnonymousAuthenticatorException;

    long count() throws AnonymousAuthenticatorException;
}
