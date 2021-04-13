package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("profile")
public class ProfileController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);
    @Autowired
    private UserRepository userRepository;

    @GetMapping()
    public ProfileResponse getProfile(Authentication authentication) {
        var userId = authentication.getName();
        try {
            final var user = userRepository.get(userId);
            return new ProfileResponse(
                    user.getProviderKey(),
                    user.getRelatedUserId() != null,
                    user.getCreatedAt().getEpochSecond()
            );
        } catch (UserException e) {
            LOGGER.warn("Could not read profile", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping()
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(Authentication authentication) {
        var userId = authentication.getName();
        try {
            final var user = userRepository.get(userId);
            userRepository.delete(user);
        } catch (UserException e) {
            LOGGER.warn("Could not delete profile", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
