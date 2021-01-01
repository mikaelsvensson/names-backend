package info.mikaelsvensson.babyname.service.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import info.mikaelsvensson.babyname.service.model.Action;
import info.mikaelsvensson.babyname.service.repository.actions.ActionException;
import info.mikaelsvensson.babyname.service.repository.actions.ActionNotFoundException;
import info.mikaelsvensson.babyname.service.repository.actions.ActionStatus;
import info.mikaelsvensson.babyname.service.repository.actions.ActionsRepository;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.auth.EmailAuthenticator;
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

    @Value("${actions.qrUrlTemplate}")
    private String qrUrlTemplate;

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
    public Action perform(Authentication authentication, @PathVariable("actionId") String actionId, @RequestBody(required = false) Map<String, String> body) {
        final var parameters = Optional.ofNullable(body).orElse(Collections.emptyMap());
        try {
            final var action = actionsRepository.get(actionId);
            if (action.getStatus() != ActionStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.GONE, "Action cannot be performed.");
            }
            try {
                return switch (action.getType()) {
                    case ADD_RELATIONSHIP -> {
                        final var userId = getUserId(authentication);
                        final var sourceUserId = action.getCreatedBy();
                        if (sourceUserId.equals(userId)) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot link to yourself.");
                        }
                        relationshipsRepository.add(
                                userRepository.get(sourceUserId),
                                userRepository.get(userId));
                        relationshipsRepository.add(
                                userRepository.get(userId),
                                userRepository.get(sourceUserId));
                        actionsRepository.setStatus(action, ActionStatus.DONE);
                        yield actionsRepository.get(actionId);
                    }
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

    private String getUserId(Authentication authentication) {
        return Optional.ofNullable(authentication).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in.")).getName();
    }
}
