package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.repository.names.*;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.repository.votes.VotesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class Recommender {

    final private NamesRepository namesRepository;
    final private VotesRepository votesRepository;
    final private RelationshipsRepository relationshipsRepository;
    final private ScbNameImporter scbNameImporter;
    final private SyllableUpdater syllableUpdater;
    final private boolean isUnigramConsidered;

    public Recommender(NamesRepository namesRepository,
                       VotesRepository votesRepository,
                       RelationshipsRepository relationshipsRepository,
                       ScbNameImporter scbNameImporter,
                       SyllableUpdater syllableUpdater,
                       @Value("${recommender.unigramConsidered}") boolean isUnigramConsidered) {
        this.namesRepository = namesRepository;
        this.votesRepository = votesRepository;
        this.relationshipsRepository = relationshipsRepository;
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

    public List<Name> getRecommendation(User user, Set<FilterAttributeNumeric> numericFilters) throws RecommenderException {
        try {
            final var ngramScores = this.getNgramScores(user);

            final var namesToRecommend = new TreeSet<>(Comparator.comparingDouble(NameScore::getScore).reversed());

            getNamesToScore(user, numericFilters, name -> {
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
                        nameScore.name.addAttribute(new AttributeNumeric(
                                AttributeKey.RECOMMENDATION_VALUE,
                                normalizedNgramScore));
                        return nameScore.name;
                    })
                    .collect(Collectors.toList());
        } catch (VoteException | RelationshipException | NameException e) {
            throw new RecommenderException(e);
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }

    private void getNamesToScore(User user, Set<FilterAttributeNumeric> numericFilters, Consumer<Name> nameConsumer) throws RelationshipException, NameException {
        namesRepository.all(
                getNameOwnerUserIds(user),
                null,
                0,
                Integer.MAX_VALUE,
                null,
                numericFilters,
                Collections.singleton(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.NOT_YET_VOTED)),
                nameConsumer);
    }

    private HashSet<String> getNameOwnerUserIds(User user) throws RelationshipException {
        final var userIds = new HashSet<String>();
        userIds.add(scbNameImporter.getUser().getId());
        userIds.add(syllableUpdater.getUser().getId());

        userIds.add(user.getId());
        userIds.addAll(relationshipsRepository.getRelatedUsers(user).stream().map(User::getId).collect(Collectors.toSet()));
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
