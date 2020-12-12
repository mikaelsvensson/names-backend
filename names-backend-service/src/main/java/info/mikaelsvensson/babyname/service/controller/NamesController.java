package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.*;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("names")
public class NamesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamesController.class);

    @Autowired
    private NamesRepository namesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RelationshipsRepository relationshipsRepository;

    @Autowired
    private VotesRepository votesRepository;

    @Autowired
    private SimilarityCalculator similarityCalculator;

    @Autowired
    private SyllableUpdater syllableUpdater;

    public NamesController() throws IOException {
    }

    @Autowired
    private ScbNameImporter scbNameImporter;

    @GetMapping
    public SearchResult get(
            Authentication authentication,
            @RequestParam(name = "name-prefix", required = false) String namePrefix,
            @RequestParam(name = "votes-filter", required = false) VotesFilter votesFilter,
            @RequestParam(name = "attribute-filter", required = false) Set<String> attributeFilterSpecs,
            @RequestParam(name = "result-offset", required = false, defaultValue = "0") int offset,
            @RequestParam(name = "result-count", required = false, defaultValue = "500") int limit
    ) {
        final var userId = authentication.getName();
        try {
            var userIds = new HashSet<String>();
            userIds.add(scbNameImporter.getUser().getId());
            userIds.add(syllableUpdater.getUser().getId());

            var userVotes = new ArrayList<Vote>();
            var partnerVotes = new ArrayList<Vote>();

            var voteFilters = new HashSet<FilterVote>();
            if (userId != null) {
                final var user = userRepository.get(userId);
                userIds.add(user.getId());
                final var partnerUsers = relationshipsRepository.getRelatedUsers(user);
                final var partnerUserIds = partnerUsers.stream().map(User::getId).collect(Collectors.toSet());
                userIds.addAll(partnerUserIds);

                userVotes.addAll(votesRepository.all(user));
                for (User partnerUser : partnerUsers) {
                    partnerVotes.addAll(votesRepository.all(partnerUser));
                }

                if (votesFilter != null) {
                    switch (votesFilter) {
                        case MY_VOTES -> voteFilters.add(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.ANY_VOTE));
                        case ALL_OUR_VOTES -> {
                            var userAndPartnerIds = new HashSet<String>();
                            userAndPartnerIds.add(userId);
                            userAndPartnerIds.addAll(partnerUserIds);
                            voteFilters.add(new FilterVote(userAndPartnerIds, FilterVoteCondition.ANY_VOTE));
                        }
                        case NEW_PARTNER_VOTES -> {
                            if (partnerUserIds.isEmpty()) {
                                return new SearchResult(Collections.emptyList(), true); // No partner, no results.
                            }
                            voteFilters.add(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.NOT_YET_VOTED));
                            voteFilters.add(new FilterVote(partnerUserIds, FilterVoteCondition.ANY_VOTE));
                        }
                        case SHARED_FAVOURITES -> {
                            if (partnerUserIds.isEmpty()) {
                                return new SearchResult(Collections.emptyList(), true); // No partner, no results.
                            }
                            voteFilters.add(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.POSITIVE_VOTE));
                            voteFilters.add(new FilterVote(partnerUserIds, FilterVoteCondition.POSITIVE_VOTE));
                        }
                    }
                }
            }

            final var numericFilters = Optional.ofNullable(attributeFilterSpecs).orElse(Collections.emptySet()).stream()
                    .map(attributeFilterSpec -> attributeFilterSpec.split(":"))
                    .filter(specFields -> specFields.length == 3)
                    .map(specFields -> new FilterAttributeNumeric(
                            AttributeKey.valueOf(specFields[0]),
                            NumericOperator.valueOf(specFields[1]),
                            Double.parseDouble(specFields[2])
                    ))
                    .collect(Collectors.toSet());

            final var names = namesRepository.all(
                    userIds,
                    namePrefix,
                    offset,
                    limit + 1,
                    null,
                    numericFilters,
                    voteFilters);
            final var isLast = names.size() < limit + 1;
            final var returnedNames = isLast ? names : names.subList(0, limit);
            return new SearchResult(
                    returnedNames
                            .stream()
                            .map(name -> new ExtendedName(
                                    name.getName(),
                                    name.getId(),
                                    name.getAttributes(),
                                    userVotes.stream()
                                            .filter(vote -> vote.getNameId().equals(name.getId()))
                                            .findFirst()
                                            .map(Vote::getValue)
                                            .orElse(null),
                                    partnerVotes.stream()
                                            .filter(vote -> vote.getNameId().equals(name.getId()))
                                            .findFirst()
                                            .map(Vote::getValue)
                                            .orElse(null)
                            ))
                            .collect(Collectors.toList()),
                    isLast);
        } catch (NameException | UserException | RelationshipException | VoteException e) {
            LOGGER.warn("Could not search for name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Name createName(Authentication authentication, @RequestBody Name nameBase) {
        var userId = authentication.getName();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

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

    @GetMapping("{nameId}")
    public Name get(
            @PathVariable(name = "nameId", required = false) String nameId
    ) {
        try {
            return namesRepository.get(nameId);
        } catch (NameException e) {
            LOGGER.warn("Could not search for name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("{nameId}/similar")
    public List<Map<String, Object>> getSimilar(Authentication authentication, @PathVariable("nameId") String nameId) {
        var userId = authentication.getName();
        try {
            final var refName = namesRepository.get(nameId);
            final var otherNames = namesRepository.all(null, null, 0, Integer.MAX_VALUE, null, null, null)
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

    public static class ExtendedName extends Name {
        private final Long userVoteValue;
        private final Long partnerVoteValue;

        public ExtendedName(String name, String id, Set<Attribute<?>> attributes, Long userVoteValue, Long partnerVoteValue) {
            super(name, id, attributes);
            this.userVoteValue = userVoteValue;
            this.partnerVoteValue = partnerVoteValue;
        }

        public Long getUserVoteValue() {
            return userVoteValue;
        }

        public Long getPartnerVoteValue() {
            return partnerVoteValue;
        }
    }

    public static class SearchResult {
        public final List<ExtendedName> names;
        public final boolean isLast;

        public SearchResult(List<ExtendedName> names, boolean isLast) {
            this.names = names;
            this.isLast = isLast;
        }
    }
}
