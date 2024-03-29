package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.auth.*;
import info.mikaelsvensson.babyname.service.util.metrics.MetricEvent;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@RestController
@RequestMapping("token")
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthJwtService authJwtService;

    @Autowired
    private FacebookAuthenticator facebookAuthenticator;

    @Autowired
    private EmailAuthenticator emailAuthenticator;

    @Autowired
    private AnonymousAuthenticator anonymousAuthenticator;

    @Autowired
    private Metrics metrics;

//    @PostConstruct
//    private void debug() {
//        try {
//            userRepository.all().stream().filter(user -> user.getProviderKey() == UserProvider.ANONYMOUS).forEach(user -> {
//                LOGGER.info(String.format("Token for %s: %s", user.getId(), authJwtService.encode(new JwtUser(user.getId()))));
//            });
//        } catch (UserException e) {
//            LOGGER.warn("Failed to print tokens", e);
//        }
//    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthTokenResponse auth(@RequestBody AuthRequest request) {
        try {
            var authenticator = switch (request.provider) {
                case INTERNAL -> throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Unsupported provider");
                case ANONYMOUS -> anonymousAuthenticator;
                case FACEBOOK -> facebookAuthenticator;
                case EMAIL -> emailAuthenticator;
            };

            var providerUserId = authenticator.getId(request.data);

            final var user = getOrCreateUser(request.provider, providerUserId);

            metrics.logEvent(MetricEvent.LOG_IN);

            return new AuthTokenResponse(authJwtService.encode(new JwtUser(user.getId())));
        } catch (UserAuthenticatorException e) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }
    }

    private User getOrCreateUser(UserProvider provider, String providerUserId) {
        try {
            return userRepository.getByProvider(provider, providerUserId);
        } catch (UserException e) {
            try {
                User user = userRepository.addFromProvider(provider, providerUserId);
                metrics.logEvent(MetricEvent.USER_ADDED);
                return user;
            } catch (UserException userException) {
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

    @PostMapping("email-verification")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendEmailVerification(@RequestBody VerifyEmailRequest request) {
        try {
            emailAuthenticator.sendEmailVerification(request.emailAddress, request.redirectTo);
        } catch (UserAuthenticatorException e) {
            LOGGER.warn("Could not start email verification", e);
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not start email verification");
        }
    }
}
