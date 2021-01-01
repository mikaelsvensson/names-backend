package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.actions.ActionsRepository;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("users")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RelationshipsRepository relationshipsRepository;

    @Autowired
    private ActionsRepository actionsRepository;

    @PostMapping
    public User create() {
        try {
            return userRepository.add();
        } catch (UserException e) {
            LOGGER.warn("Could not register user", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("relationships")
    public List<User> getRelationships(Authentication authentication) {
        try {
            var userId = authentication.getName();
            return relationshipsRepository.getRelatedUsers(userRepository.get(userId));
        } catch (UserException | RelationshipException e) {
            LOGGER.warn("Could not read relationships", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
