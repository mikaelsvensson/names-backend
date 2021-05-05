package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.util.auth.AnonymousAuthenticator;
import info.mikaelsvensson.babyname.service.util.auth.UserAuthenticatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

@RestController
@RequestMapping("anonymous-authenticator")
public class AnonymousAuthenticatorController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousAuthenticatorController.class);

    private final AnonymousAuthenticator anonymousAuthenticator;

    public AnonymousAuthenticatorController(@Autowired AnonymousAuthenticator anonymousAuthenticator) {
        this.anonymousAuthenticator = anonymousAuthenticator;
    }

    @PostMapping("id")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAnonymousUserResponse createAnonymousUser() {
        try {
            final var id = anonymousAuthenticator.createId();
            return new CreateAnonymousUserResponse(id);
        } catch (UserAuthenticatorException e) {
            LOGGER.warn("Could not create anonymous user.", e);
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create anonymous user.");
        }
    }
}
