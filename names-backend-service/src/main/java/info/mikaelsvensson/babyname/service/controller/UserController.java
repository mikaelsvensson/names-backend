package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.repository.actions.ActionException;
import info.mikaelsvensson.babyname.service.repository.actions.ActionStatus;
import info.mikaelsvensson.babyname.service.repository.actions.ActionsRepository;
import info.mikaelsvensson.babyname.service.repository.names.*;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.repository.votes.VotesRepository;
import info.mikaelsvensson.babyname.service.util.ScbNameImporter;
import info.mikaelsvensson.babyname.service.util.SyllableUpdater;
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
    private SimilarityCalculator similarityCalculator;

    @Autowired
    private RelationshipsRepository relationshipsRepository;

    @Autowired
    private ActionsRepository actionsRepository;

    @Autowired
    private ScbNameImporter scbNameImporter;

    @Autowired
    private SyllableUpdater syllableUpdater;

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

    @GetMapping("/{userId}/names/{nameId}/similar")
    public List<Map<String, Object>> getSimilar(@PathVariable("userId") String userId, @PathVariable("nameId") String nameId) {
        try {
            final var refName = namesRepository.get(nameId);
            final var otherNames = namesRepository.all(null, null, Integer.MAX_VALUE, null, null)
                    .stream()
                    .filter(name -> !name.getId().equals(refName.getId()))
                    .collect(Collectors.toList());

            return similarityCalculator.get(refName, otherNames)
                    .entrySet()
                    .stream()
                    .map(nameMapEntry -> Map.of(
                            "id", nameMapEntry.getKey().getId(),
                            "name", nameMapEntry.getKey().getName(),
                            "values", nameMapEntry.getValue()
                    ))
                    .collect(Collectors.toList());
        } catch (NameException e) {
            LOGGER.warn("Could get similar names", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/{userId}/votes/{nameId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void setVotes(@PathVariable("userId") String userId, @PathVariable("nameId") String nameId, @RequestBody Vote vote) {
        try {
            votesRepository.set(userRepository.get(userId), namesRepository.get(nameId), vote.getValue());
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
            @RequestParam(name = "attribute-filter", required = false) Set<String> attributeFilterSpecs,
            @RequestParam(name = "result-count", required = false, defaultValue = "500") int limit
    ) {
        try {
            final var user = userRepository.get(userId);
            var userIds = new HashSet<String>();
            userIds.add(user.getId());
            userIds.add(scbNameImporter.getUser().getId());
            userIds.add(syllableUpdater.getUser().getId());
            userIds.addAll(relationshipsRepository.getRelatedUsers(user).stream().map(User::getId).collect(Collectors.toList()));

            final var numericFilters = Optional.ofNullable(attributeFilterSpecs).orElse(Collections.emptySet()).stream().map(attributeFilterSpec -> attributeFilterSpec.split(":")).filter(specFields -> specFields.length == 3).map(specFields -> new AttributeFilterNumeric(
                    AttributeKey.valueOf(specFields[0]),
                    NumericOperator.valueOf(specFields[1]),
                    Double.parseDouble(specFields[2])
            )).collect(Collectors.toSet());

            return new NamesController.SearchResult(namesRepository.all(
                    userIds,
                    namePrefix,
                    limit,
                    Set.of(Optional.ofNullable(votedBy).map(s -> s.split(",")).orElse(new String[]{})),
                    numericFilters));
        } catch (NameException | RelationshipException | UserException e) {
            LOGGER.warn("Could search for name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/{userId}/names")
    @ResponseStatus(HttpStatus.CREATED)
    public Name createName(@PathVariable("userId") String userId, @RequestBody Name nameBase) {
        try {
            final var user = userRepository.get(userId);
            final var name = namesRepository.add(nameBase.getName(), user, Collections.emptySet());
            votesRepository.set(user, name, Vote.VALUE_UP);
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
