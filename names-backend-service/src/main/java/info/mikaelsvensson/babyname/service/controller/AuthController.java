package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.AuthJwtService;
import info.mikaelsvensson.babyname.service.util.JwtUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;

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
                case INTERNAL, ANONYMOUS, EMAIL -> throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Unsupported provider");
                case FACEBOOK -> facebookAuthenticator;
            };

            var providerUserId = authenticator.getId(request.data);

            final var user = getOrCreateUser(request.provider, providerUserId);

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
                return userRepository.addFromProvider(provider, providerUserId);
            } catch (UserException userException) {
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }
}
