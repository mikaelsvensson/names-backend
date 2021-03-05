package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.RecommendationProperties;
import info.mikaelsvensson.babyname.service.repository.names.Country;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.names.request.*;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.repository.votes.VotesRepository;
import info.mikaelsvensson.babyname.service.util.nameprovider.AbstractNameImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class Recommender {

    private static final Logger LOGGER = LoggerFactory.getLogger(Recommender.class);

    final private NamesRepository namesRepository;
    final private UserRepository userRepository;
    final private AbstractNameImporter[] nameImporters;
    final private boolean isUnigramConsidered;

    public Recommender(@Autowired NamesRepository namesRepository,
                       @Autowired UserRepository userRepository,
                       @Autowired AbstractNameImporter[] nameImporters,
                       @Value("${recommender.unigramConsidered}") boolean isUnigramConsidered) {
        this.namesRepository = namesRepository;
        this.userRepository = userRepository;
        this.nameImporters = nameImporters;
        this.isUnigramConsidered = isUnigramConsidered;
    }

    private static class NameScore {
        private final Name name;
        private final Double score;

        public NameScore(Name name, Double score) {
            this.name = name;
            this.score = score;
        }

        public Double getScore() {
            return score;
        }
    }

    public List<Name> getRecommendation(User user, NamesRequest baseRequest) throws RecommenderException {
        try {
            final var ngramScores = this.getNgramScores(user, baseRequest);

            final var namesToRecommend = new TreeSet<>(Comparator.comparingDouble(NameScore::getScore).reversed());

            getNamesToScore(user, baseRequest, FilterVoteCondition.NOT_YET_VOTED, name -> {
                final var score = this.getScore(name.getName(), ngramScores);

                final var nameScore = new NameScore(name, score);
                if (namesToRecommend.size() < 100) {
                    namesToRecommend.add(nameScore);
                } else {
                    NameScore lowestScoreName = namesToRecommend.last();
                    if (score > lowestScoreName.score) {
                        namesToRecommend.add(nameScore);
                        namesToRecommend.remove(lowestScoreName);
                    }
                }
            });

            final var ngramScoreMin = namesToRecommend.stream().mapToDouble(NameScore::getScore).min().orElse(Integer.MIN_VALUE);
            final var ngramScoreMax = namesToRecommend.stream().mapToDouble(NameScore::getScore).max().orElse(Integer.MAX_VALUE);

            return namesToRecommend
                    .stream()
                    .map(nameScore -> {
                        final var normalizedNgramScore = (nameScore.score - ngramScoreMin) / (ngramScoreMax - ngramScoreMin);
                        nameScore.name.setRecommendation(new RecommendationProperties(normalizedNgramScore));
                        return nameScore.name;
                    })
                    .collect(Collectors.toList());
        } catch (UserException | NameException e) {
            throw new RecommenderException(e);
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }

    private void getNamesToScore(User user, NamesRequest baseRequest, FilterVoteCondition filterVoteCondition, Consumer<Name> nameConsumer) throws NameException, UserException {
        final var request = new NamesRequest()
                .basic(new BasicNameFacet()
                        .nameOwnerUserIds(getNameOwnerUserIds(user)))
                .votes(new VotesNameFacet()
                        .selfUserId(user.getId())
                        .filterVotes(Collections.singleton(new FilterVote(Collections.singleton(user.getId()), filterVoteCondition))))
                .limit(Integer.MAX_VALUE)
                .offset(0);

        if (baseRequest.demographics != null) {
            for (Country country : Country.values()) {
                if (baseRequest.demographics.containsKey(country)) {
                    request.demographics(baseRequest.demographics.get(country), country);
                }
            }
        }
        if (baseRequest.metrics != null) {
            request.metrics(baseRequest.metrics);
        }
        namesRepository.all(request, nameConsumer);
    }

    private HashSet<String> getNameOwnerUserIds(User user) throws UserException {
        final var userIds = new HashSet<String>();
        Stream.of(nameImporters).forEach(importer -> userIds.add(importer.getUser().getId()));

        userIds.add(user.getId());
        if (user.getRelatedUserId() != null) {
            userIds.add(userRepository.get(user.getRelatedUserId()).getId());
        }
        return userIds;
    }

    private Double getScore(String name, Map<String, Integer> stats) {
        return ngrams(name)
                .stream()
                .mapToDouble(value -> stats.getOrDefault(value, 0))
                .sum();
    }

    private Map<String, Integer> getNgramScores(User user, NamesRequest baseRequest) throws NameException, UserException {
        final var counts = new HashMap<String, Integer>();

        final var voteByName = new HashMap<String, Long>();
        getNamesToScore(
                user,
                baseRequest,
                FilterVoteCondition.NOT_NEUTRAL_VOTE,
                name -> voteByName.put(
                        name.getName().toLowerCase(),
                        name.getVotes().getSelfVoteValue().longValue()
                )
        );

        final var voteSumMax = voteByName.size() * 100;
        final var voteSumMin = voteByName.size() * -100;
        final var voteSum = voteByName.values().stream().mapToLong(Long::longValue).sum();
        final var voteAverageNormalized = 1.0 * (voteSum - voteSumMin) / (voteSumMax - voteSumMin);
        final var voteAveragePoints = (int) (-100.0 + 200.0 * voteAverageNormalized);

        for (final var entry : voteByName.entrySet()) {
            final var name = entry.getKey();
            final var votePoints = entry.getValue().intValue();
            ngrams(name).forEach(nameSubstring -> {
                counts.put(
                        nameSubstring,
                        counts.getOrDefault(nameSubstring, 0) + (votePoints - voteAveragePoints) * (nameSubstring.length() - 2));
            });
        }

        return counts;
    }

    private Collection<String> ngrams(String input) {
        final var strings = new ArrayList<String>();
        for (int substringLength = isUnigramConsidered ? 1 : 2; substringLength <= 3; substringLength++) {
            for (int pos = 0; pos < input.length() - substringLength + 1; pos++) {
                strings.add(
                        (pos > 0 ? "*" : " ") +
                                input.substring(pos, pos + substringLength) +
                                (pos < input.length() - substringLength ? "*" : " ")
                );
            }
        }
        return strings;
    }
}
