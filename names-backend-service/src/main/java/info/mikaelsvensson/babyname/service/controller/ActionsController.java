package info.mikaelsvensson.babyname.service.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import info.mikaelsvensson.babyname.service.model.Action;
import info.mikaelsvensson.babyname.service.model.ActionStatus;
import info.mikaelsvensson.babyname.service.model.ActionType;
import info.mikaelsvensson.babyname.service.repository.actions.*;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.auth.EmailAuthenticator;
import info.mikaelsvensson.babyname.service.util.metrics.MetricEvent;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("actions")
public class ActionsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionsController.class);

    @Autowired
    private ActionsRepository actionsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RelationshipsRepository relationshipsRepository;

    @Autowired
    private EmailAuthenticator emailAuthenticator;

    @Value("${actions.qrUrlTemplate}")
    private String qrUrlTemplate;

    @Autowired
    private Metrics metrics;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Action createAction(Authentication authentication, @RequestBody Action action) {
        try {
            var userId = authentication.getName();
            return actionsRepository.add(userRepository.get(userId), action.getType(), action.getParameters(), ActionStatus.PENDING);
        } catch (UserException | ActionException e) {
            LOGGER.warn("Could not create relationship", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(value = "{actionId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<BufferedImage> getQrCode(@PathVariable("actionId") String actionId) {
        try {
            final Action action = actionsRepository.get(actionId);

            QRCodeWriter barcodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = barcodeWriter.encode(qrUrlTemplate.replace("{actionId}", action.getId()), BarcodeFormat.QR_CODE, 200, 200);

            return ResponseEntity.ok().body(MatrixToImageWriter.toBufferedImage(bitMatrix));
        } catch (ActionException | WriterException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("{actionId}/invocation")
    public Action perform(Authentication authentication,
                          @PathVariable("actionId") String actionId,
                          @RequestBody(required = false) Map<String, String> body) {
        final var parameters = Optional.ofNullable(body).orElse(Collections.emptyMap());
        try {
            final var action = actionsRepository.get(actionId);
            if (action.getStatus() != ActionStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.GONE, "Action cannot be performed.");
            }
            try {
                return switch (action.getType()) {
                    case ADD_RELATIONSHIP -> handleAddRelationship(authentication, action);
                    case DELETE_DATA_REQUEST_STATUS -> handleDeleteDataRequestStatus(action);
                    case DELETE_DATA_CONFIRMATION -> handleDeleteDataConfirmation(action);
                    case VERIFY_EMAIL -> handleVerifyEmail(action);
                };
            } catch (RelationshipException | UserException | ActionException e) {
                LOGGER.warn("Could not perform action.", e);
                actionsRepository.setStatus(action, ActionStatus.FAILED);
                return actionsRepository.get(actionId);
            }
        } catch (ActionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ActionException e) {
            LOGGER.warn("Could not perform action.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Action handleDeleteDataConfirmation(Action action) {
        try {
            final var sourceUserId = action.getCreatedBy();
            LOGGER.info("User {} requested to be deleted from the system.", sourceUserId);
            userRepository.delete(userRepository.get(sourceUserId));
            LOGGER.info("User {} was deleted from the system.", sourceUserId);
            return new Action(null, ActionType.DELETE_DATA_CONFIRMATION, sourceUserId, ActionStatus.DONE, null);
        } catch (UserException e) {
            LOGGER.warn("Could not delete user.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Action handleDeleteDataRequestStatus(Action action) {
        try {
            return actionsRepository.get(action.getId());
        } catch (ActionException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private Action handleVerifyEmail(Action action) throws ActionException {
        final var emailAddress = action.getParameters().get("emailAddress");
        if (emailAddress == null) {
            throw new ActionException("No email address specified.");
        }
        final var redirectTo = action.getParameters().get("redirectTo");
        actionsRepository.setStatus(action, ActionStatus.DONE);
        return new VerifyEmailResult(
                actionsRepository.get(action.getId()),
                emailAuthenticator.getTokenForEmailAddress(emailAddress),
                redirectTo);
    }

    private Action handleAddRelationship(Authentication authentication, Action action) throws RelationshipException, UserException, ActionException {
        final var userId = getUserId(authentication);
        final var sourceUserId = action.getCreatedBy();
        if (sourceUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot link to yourself.");
        }
        relationshipsRepository.connect(
                userRepository.get(sourceUserId),
                userRepository.get(userId));
        metrics.logEvent(MetricEvent.RELATIONSHIP_ADDED);
        actionsRepository.setStatus(action, ActionStatus.DONE);
        return actionsRepository.get(action.getId());
    }

    private String getUserId(Authentication authentication) {
        return Optional.ofNullable(authentication).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in.")).getName();
    }
}
