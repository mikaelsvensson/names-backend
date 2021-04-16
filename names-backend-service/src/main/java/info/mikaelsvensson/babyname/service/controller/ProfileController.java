package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.ActionStatus;
import info.mikaelsvensson.babyname.service.model.ActionType;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.repository.actions.ActionException;
import info.mikaelsvensson.babyname.service.repository.actions.ActionsRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.auth.FacebookAuthenticator;
import info.mikaelsvensson.babyname.service.util.auth.UserAuthenticatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("profile")
public class ProfileController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActionsRepository actionsRepository;

    @Autowired
    private FacebookAuthenticator facebookAuthenticator;

    @Value("${facebook.deleteDataCallbackUrlTemplate}")
    private String facebookDeleteDataCallbackUrlTemplate;

    @Value("${facebook.deleteDataStatusUrlTemplate}")
    private String facebookDeleteDataStatusUrlTemplate;

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

    @PostMapping("delete-facebook-data-request")
    public FacebookDeleteDataResponse deleteFacebookData(@RequestBody FacebookDeleteDataRequest request) {
        try {
            final var facebookUserId = facebookAuthenticator.getId(request.signed_request);
            LOGGER.info("Facebook requested that data be deleted for a user.");
            final var user = userRepository.getByProvider(UserProvider.FACEBOOK, facebookUserId);
            LOGGER.info("Facebook user id matches user {}.", user.getId());

            final var confirmAction = actionsRepository.add(
                    user,
                    ActionType.DELETE_DATA_CONFIRMATION,
                    null,
                    ActionStatus.PENDING);

            final var statusAction = actionsRepository.add(
                    user,
                    ActionType.DELETE_DATA_REQUEST_STATUS,
                    Map.of("confirmActionId", confirmAction.getId()),
                    ActionStatus.PENDING);

            LOGGER.info("Created two actions for completing the deletion process. Check status action: {}. Confirm deletion action: {}", statusAction.getId(), confirmAction.getId());

            return new FacebookDeleteDataResponse(facebookDeleteDataCallbackUrlTemplate.replace("{actionId}", statusAction.getId()), statusAction.getId());
        } catch (UserAuthenticatorException | UserException | ActionException e) {
            LOGGER.warn("Could not start data deletion process", e);
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not start data deletion process");
        }
    }

    @GetMapping("delete-facebook-data-request/{actionId}")
    public ResponseEntity<Void> redirectToRequestStatus(@PathVariable("actionId") String actionId) {
        try {
            final var action = actionsRepository.get(actionId);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(facebookDeleteDataStatusUrlTemplate.replace("{actionId}", action.getId())))
                    .build();
        } catch (ActionException e) {
            LOGGER.warn("Could not start data deletion process", e);
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not start data deletion process");
        }
    }
}
