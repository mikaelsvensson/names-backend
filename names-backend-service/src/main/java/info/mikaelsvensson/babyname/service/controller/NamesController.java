package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.repository.names.Country;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.names.request.*;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.repository.votes.VotesRepository;
import info.mikaelsvensson.babyname.service.util.Recommender;
import info.mikaelsvensson.babyname.service.util.RecommenderException;
import info.mikaelsvensson.babyname.service.util.SyllableUpdater;
import info.mikaelsvensson.babyname.service.util.nameprovider.AbstractNameImporter;
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
import java.util.stream.Stream;

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
    private SyllableUpdater syllableUpdater;

    @Autowired
    private AbstractNameImporter[] nameImporters;

    @Autowired
    private Recommender recommender;

    public NamesController() throws IOException {
    }

    @GetMapping
    public SearchResult get(
            Authentication authentication,
            @RequestParam(name = "name-prefix", required = false) String namePrefix,
            @RequestParam(name = "votes-filter", required = false) VotesFilter votesFilter,
            @RequestParam(name = "attribute-filter", required = false) Set<String> attributeFilterSpecs,
            @RequestParam(name = "result-offset", required = false, defaultValue = "0") int offset,
            @RequestParam(name = "result-count", required = false, defaultValue = "500") int limit,
            @RequestParam(name = "sort-order", required = false, defaultValue = "NAME") String sortOrderSpec
    ) {
        try {
            final var strings = sortOrderSpec.split(":");
            final var request = new NamesRequest()
                    .basic(new BasicNameFacet())
                    .offset(offset)
                    .sortOrder(SortOrder.valueOf(strings[0]), strings.length > 1 ? strings[1] : null)
                    .limit(limit + 1);

            final var userIds = new HashSet<String>();
            Stream.of(nameImporters).forEach(importer -> userIds.add(importer.getUser().getId()));

            final var userId = getUserId(authentication);
            final var user = userId != null ? userRepository.get(userId) : null;
            final var partnerUserId = user != null ? user.getRelatedUserId() : null;
            if (user != null) {
                userIds.add(user.getId());
                if (partnerUserId != null) {
                    userIds.add(partnerUserId);
                }
                final var votesNameFacet = new VotesNameFacet()
                        .selfUserId(user.getId())
                        .partnerUserId(partnerUserId);
                if (votesFilter != null) {
                    final var voteFilters = new HashSet<FilterVote>();
                    switch (votesFilter) {
                        case MY_VOTES -> voteFilters.add(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.ANY_VOTE));
                        case MY_FAVOURITES -> voteFilters.add(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.POSITIVE_VOTE));
                        case ALL_OUR_VOTES -> {
                            voteFilters.add(new FilterVote(
                                    partnerUserId != null
                                            ? Set.of(userId, partnerUserId)
                                            : Set.of(userId),
                                    FilterVoteCondition.ANY_VOTE));
                        }
                        case NEW_PARTNER_VOTES -> {
                            if (partnerUserId == null) {
                                return new SearchResult(Collections.emptyList(), true); // No partner, no results.
                            }
                            voteFilters.add(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.NOT_YET_VOTED));
                            voteFilters.add(new FilterVote(Collections.singleton(partnerUserId), FilterVoteCondition.ANY_VOTE));
                        }
                        case SHARED_FAVOURITES -> {
                            if (partnerUserId == null) {
                                return new SearchResult(Collections.emptyList(), true); // No partner, no results.
                            }
                            voteFilters.add(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.POSITIVE_VOTE));
                            voteFilters.add(new FilterVote(Collections.singleton(partnerUserId), FilterVoteCondition.POSITIVE_VOTE));
                        }
                    }
                    votesNameFacet.filterVotes(voteFilters);
                }
                request.votes(votesNameFacet);
            }

            request.basic(new BasicNameFacet()
                    .nameOwnerUserIds(userIds)
                    .namePrefix(namePrefix));


            if (attributeFilterSpecs != null) {
                initRequestAttributeFilters(request, attributeFilterSpecs);
            }

            for (Country country : Country.values()) {
                if (request.demographics == null || !request.demographics.containsKey(country)) {
                    request.demographics(new PopulationNameFacet(), country);
                }
            }

            final var names = new ArrayList<Name>();
            namesRepository.all(
                    request,
                    names::add);
            final var isLast = names.size() < limit + 1;
            final var returnedNames = isLast ? names : names.subList(0, limit);
            return new SearchResult(
                    returnedNames,
                    isLast);
        } catch (NameException | UserException e) {
            LOGGER.warn("Could not search for name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void initRequestAttributeFilters(NamesRequest request, Set<String> attributeFilterSpecs) {
        final var demographicFacet = new PopulationNameFacet();
        final var metricsFacet = new MetricsNameFacet();
        attributeFilterSpecs.stream()
                .map(attributeFilterSpec -> attributeFilterSpec.split(":"))
                .filter(specFields -> specFields.length == 3)
                .forEach(specFields -> {
                    final var filter = new FilterNumeric(
                            FilterNumericOperator.valueOf(specFields[1]),
                            Double.parseDouble(specFields[2])
                    );
                    switch (AttributeKey.valueOf(specFields[0])) {
                        case SYLLABLE_COUNT:
                            metricsFacet.syllableFilter(filter);
                            request.metrics(metricsFacet);
                            break;
                        case SCB_PERCENT_WOMEN:
                            demographicFacet.percentWomenFilter(filter);
                            request.demographics(demographicFacet, Country.SWEDEN);
                            break;
                        case SCB_PERCENT_OF_POPULATION:
                            demographicFacet.percentOfPopulationFilter(filter);
                            request.demographics(demographicFacet, Country.SWEDEN);
                            break;
                    }
                });
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
            final var name = namesRepository.add(nameBase.getName(), user);
            votesRepository.set(user, name, Vote.VALUE_UP);
            return name;
        } catch (UserException | NameException | VoteException e) {
            LOGGER.warn("Could not add name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("{nameId}")
    public Name get(
            Authentication authentication,
            @PathVariable(name = "nameId", required = false) String nameId
    ) {
        try {
            var userId = getUserId(authentication);
            final var user = userId != null ? userRepository.get(userId) : null;
            return namesRepository.get(nameId, user);
        } catch (NameException | UserException e) {
            LOGGER.warn("Could not search for name", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("{nameId}/similar")
    public List<Name> getSimilar(Authentication authentication, @PathVariable("nameId") String nameId) {
        try {
            final var userId = getUserId(authentication);
            final var user = userId != null ? userRepository.get(userId) : null;

            final var similarNameIds = namesRepository.getSimilar(nameId, user);
            if (similarNameIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Similar names have not been calculated yet.");
            }
            return similarNameIds.get()
                    .stream()
                    .map(id -> {
                        try {
                            return namesRepository.get(id, user);
                        } catch (NameException e) {
                            LOGGER.warn("Could read name", e);
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (NameException | UserException e) {
            LOGGER.warn("Could get similar names", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("recommendations")
    public List<Name> getRecommendations(Authentication authentication,
                                         @RequestParam(name = "attribute-filter", required = false) Set<String> attributeFilterSpecs,
                                         @RequestParam(name = "result-count", required = false, defaultValue = "10") int limit
    ) {
        try {
            final var baseRequest = new NamesRequest().basic(new BasicNameFacet()).demographics(new PopulationNameFacet(), Country.SWEDEN);
            if (attributeFilterSpecs != null) {
                initRequestAttributeFilters(baseRequest, attributeFilterSpecs);
            }
            final var allRecommendations = recommender.getRecommendation(userRepository.get(getUserId(authentication)), baseRequest);
            return allRecommendations.subList(0, Math.min(limit, allRecommendations.size()));
        } catch (RecommenderException | UserException e) {
            LOGGER.warn("Could get recommendations", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public static class SearchResult {
        public final List<Name> names;
        public final boolean isLast;

        public SearchResult(List<Name> names, boolean isLast) {
            this.names = names;
            this.isLast = isLast;
        }
    }

    private String getUserId(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
