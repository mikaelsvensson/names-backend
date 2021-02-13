package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.RecommendationProperties;
import info.mikaelsvensson.babyname.service.repository.names.*;
import info.mikaelsvensson.babyname.service.repository.names.request.BasicNameFacet;
import info.mikaelsvensson.babyname.service.repository.names.request.NamesRequest;
import info.mikaelsvensson.babyname.service.repository.names.request.VotesNameFacet;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.repository.votes.VotesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class Recommender {

    private static final Logger LOGGER = LoggerFactory.getLogger(Recommender.class);

    final private NamesRepository namesRepository;
    final private VotesRepository votesRepository;
    final private UserRepository userRepository;
    final private ScbNameImporter scbNameImporter;
    final private SyllableUpdater syllableUpdater;
    final private boolean isUnigramConsidered;

    public Recommender(NamesRepository namesRepository,
                       VotesRepository votesRepository,
                       UserRepository userRepository,
                       ScbNameImporter scbNameImporter,
                       SyllableUpdater syllableUpdater,
                       @Value("${recommender.unigramConsidered}") boolean isUnigramConsidered) {
        this.namesRepository = namesRepository;
        this.votesRepository = votesRepository;
        this.userRepository = userRepository;
        this.scbNameImporter = scbNameImporter;
        this.syllableUpdater = syllableUpdater;
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
            final var ngramScores = this.getNgramScores(user);

            final var namesToRecommend = new TreeSet<>(Comparator.comparingDouble(NameScore::getScore).reversed());

            getNamesToScore(user, baseRequest, name -> {
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
        } catch (VoteException | UserException | NameException e) {
            throw new RecommenderException(e);
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }

    private void getNamesToScore(User user, NamesRequest baseRequest, Consumer<Name> nameConsumer) throws NameException, UserException {
        final var request = new NamesRequest()
                .basic(new BasicNameFacet()
                        .nameOwnerUserIds(getNameOwnerUserIds(user)))
                .votes(new VotesNameFacet()
                        .selfUserId(user.getId())
                        .partnerUserId(user.getRelatedUserId())
                        .filterVotes(Collections.singleton(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.NOT_YET_VOTED))))
                .limit(Integer.MAX_VALUE)
                .offset(0);

        if (baseRequest.scb != null) {
            request.scb(baseRequest.scb);
        }
        if (baseRequest.metrics != null) {
            request.metrics(baseRequest.metrics);
        }
        namesRepository.all(request, nameConsumer);
    }

    private HashSet<String> getNameOwnerUserIds(User user) throws UserException {
        final var userIds = new HashSet<String>();
        userIds.add(scbNameImporter.getUser().getId());

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

    private Map<String, Integer> getNgramScores(User user) throws VoteException, NameException {
        final var votes = this.votesRepository.all(user);

        final var allNames = namesRepository.allNames();

        LOGGER.info("Got {} names and {} votes.", allNames.size(), votes.size());

        final var counts = new HashMap<String, Integer>();
        final var voteByName = votes.stream()
                .filter(vote -> vote.getValue() != 0)
                .map(vote -> Map.entry(allNames.get(vote.getNameId()), vote.getValue()))
                .collect(Collectors.toList());

        final var voteSumMax = voteByName.size() * 100;
        final var voteSumMin = voteByName.size() * -100;
        final var voteSum = voteByName.stream().mapToLong(Map.Entry::getValue).sum();
        final var voteAverageNormalized = 1.0 * (voteSum - voteSumMin) / (voteSumMax - voteSumMin);
        final var voteAveragePoints = (int) (-100.0 + 200.0 * voteAverageNormalized);

        for (final Map.Entry<String, Long> entry : voteByName) {
            final var name = entry.getKey().toLowerCase();
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
