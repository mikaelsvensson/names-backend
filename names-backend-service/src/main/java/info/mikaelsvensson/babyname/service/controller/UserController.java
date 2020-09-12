package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.repository.*;
import info.mikaelsvensson.babyname.service.util.ScbNameImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("users")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VotesRepository votesRepository;

    @Autowired
    private NamesRepository namesRepository;

    @Autowired
    private RelationshipsRepository relationshipsRepository;

    @Autowired
    private ActionsRepository actionsRepository;

    @Autowired
    private ScbNameImporter scbNameImporter;

    @PostMapping
    public User create() {
        try {
            return userRepository.add();
        } catch (UserException e) {
            LOGGER.warn("Could not register user", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{userId}/votes")
    public Collection<Vote> getVotes(@PathVariable("userId") String userId) {
        try {
            return votesRepository.all(userRepository.get(userId));
        } catch (UserException | VoteException e) {
            LOGGER.warn("Could not read votes", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{userId}/relationships")
    public List<User> getRelationships(@PathVariable("userId") String userId) {
        try {
            return relationshipsRepository.getRelatedUsers(userRepository.get(userId));
        } catch (UserException | RelationshipException e) {
            LOGGER.warn("Could not read relationships", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/{userId}/votes/{nameId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void setVotes(@PathVariable("userId") String userId, @PathVariable("nameId") String nameId, @RequestBody Vote vote) {
        try {
            votesRepository.set(userRepository.get(userId), namesRepository.get(nameId), vote.getVoteType());
        } catch (UserException | NameException | VoteException e) {
            LOGGER.warn("Could not cast vote", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{userId}/names")
    public NamesController.SearchResult get(
            @PathVariable("userId") String userId,
            @RequestParam(name = "name-prefix", required = false) String namePrefix,
            @RequestParam(name = "voted-by", required = false) String votedBy,
            @RequestParam(name = "result-count", required = false, defaultValue = "500") int limit,
            @RequestParam(name = "popularity", required = false) CountRange countRange
    ) {
        try {
            final var user = userRepository.get(userId);
            var userIds = new HashSet<String>();
            userIds.add(user.getId());
            userIds.add(scbNameImporter.getUser().getId());
            userIds.addAll(relationshipsRepository.getRelatedUsers(user).stream().map(User::getId).collect(Collectors.toList()));
            return new NamesController.SearchResult(namesRepository.all(
                    userIds,
                    namePrefix,
                    limit,
                    countRange,
                    Set.of(Optional.ofNullable(votedBy).map(s -> s.split(",")).orElse(new String[]{}))));
        } catch (NameException | RelationshipException | UserException e) {
            LOGGER.warn("Could search for name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/{userId}/names")
    @ResponseStatus(HttpStatus.CREATED)
    public Name createName(@PathVariable("userId") String userId, @RequestBody NameBase nameBase) {
        try {
            final var name = namesRepository.add(nameBase.getName(), nameBase.isMale(), nameBase.isFemale(), nameBase.isPublic(), userId);
            votesRepository.set(userRepository.get(userId), name, VoteType.UP);
            return name;
        } catch (UserException | NameException | VoteException e) {
            LOGGER.warn("Could not add name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/{userId}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public Action createAction(@PathVariable("userId") String userId, @RequestBody Action action) {
        try {
            return actionsRepository.add(userRepository.get(userId), action.getType(), action.getParameters(), ActionStatus.PENDING);
        } catch (UserException | ActionException e) {
            LOGGER.warn("Could not create relationship", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
