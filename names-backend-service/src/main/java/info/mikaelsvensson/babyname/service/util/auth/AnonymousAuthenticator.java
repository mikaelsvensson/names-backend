package info.mikaelsvensson.babyname.service.util.auth;

import info.mikaelsvensson.babyname.service.repository.actions.DbActionsRepository;
import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.AnonymousAuthenticatorException;
import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.AnonymousAuthenticatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnonymousAuthenticator implements UserAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbActionsRepository.class);

    private final AnonymousAuthenticatorRepository repository;
    private final long userLimit;

    public AnonymousAuthenticator(
            @Autowired AnonymousAuthenticatorRepository repository,
            @Value("${anonymousAuthenticator.userLimit}") long userLimit) {
        this.repository = repository;
        this.userLimit = userLimit;
    }

    @Override
    public String getId(String token) throws UserAuthenticatorException {
        try {
            if (repository.exists(token)) {
                repository.logUse(token);
                return token;
            } else {
                throw new UserAuthenticatorException("Not found");
            }
        } catch (AnonymousAuthenticatorException e) {
            LOGGER.warn("Could not authenticate.", e);
            throw new UserAuthenticatorException("Could not authenticate.");
        }
    }

    public String createId() throws UserAuthenticatorException {
        try {
            if (repository.count() < userLimit) {
                return repository.create();
            } else {
                throw new UserAuthenticatorException("No more anonymous users are allowed.");
            }
        } catch (AnonymousAuthenticatorException e) {
            LOGGER.warn("Anonymous user could not be created.", e);
            throw new UserAuthenticatorException("Could not create user");
        }
    }
}
