package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.AttributeKey;
import info.mikaelsvensson.babyname.service.model.AttributeNumeric;
import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.names.*;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.repository.votes.VotesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
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

    public List<Name> getRecommendation(User user, Set<FilterAttributeNumeric> numericFilters) throws RecommenderException {
        try {
            final List<Name> namesToScore = getNamesToScore(user, numericFilters);

            scoreNames(user, namesToScore);

            return namesToScore
                    .stream()
                    .sorted((o1, o2) ->
                            ((Double) (o2
                                    .getAttribute(AttributeKey.RECOMMENDATION_VALUE)
                                    .orElse(new AttributeNumeric(AttributeKey.RECOMMENDATION_VALUE, 0.0)))
                                    .getValue()
                            ).compareTo((Double) (o1
                                    .getAttribute(AttributeKey.RECOMMENDATION_VALUE)
                                    .orElse(new AttributeNumeric(AttributeKey.RECOMMENDATION_VALUE, 0.0)))
                                    .getValue()))
                    .limit(100)
                    .collect(Collectors.toList());
        } catch (VoteException | RelationshipException | NameException e) {
            throw new RecommenderException(e);
        }
    }

    private List<Name> getNamesToScore(User user, Set<FilterAttributeNumeric> numericFilters) throws RelationshipException, NameException {

        return namesRepository.all(
                getNameOwnerUserIds(user),
                null,
                0,
                Integer.MAX_VALUE,
                null,
                numericFilters,
                Collections.singleton(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.NOT_YET_VOTED)));
    }

    private HashSet<String> getNameOwnerUserIds(User user) throws RelationshipException {
        final var userIds = new HashSet<String>();
        userIds.add(scbNameImporter.getUser().getId());
        userIds.add(syllableUpdater.getUser().getId());

        userIds.add(user.getId());
        userIds.addAll(relationshipsRepository.getRelatedUsers(user).stream().map(User::getId).collect(Collectors.toSet()));
        return userIds;
    }

    private void scoreNames(User user, List<Name> namesToScore) throws VoteException, RelationshipException, NameException {
        final var stats = this.getNgramStats(user);

        final var rawNgramScores = new HashMap<String, Double>();
        namesToScore.forEach(name -> {
            final var ngramScore = this.getNgramScore(name, stats);
            rawNgramScores.put(name.getId(), ngramScore);
        });

        final var ngramScoreMin = rawNgramScores.values().stream().mapToDouble(value -> value).min().orElse(Integer.MIN_VALUE);
        final var ngramScoreMax = rawNgramScores.values().stream().mapToDouble(value -> value).max().orElse(Integer.MAX_VALUE);

        namesToScore.forEach(name -> {
            final var normalizedNgramScore = (rawNgramScores.get(name.getId()) - ngramScoreMin) / (ngramScoreMax - ngramScoreMin);
            name.addAttribute(new AttributeNumeric(
                    AttributeKey.RECOMMENDATION_VALUE,
                    normalizedNgramScore));
        });
    }

    private Double getNgramScore(Name name, Map<String, Integer> stats) {
        return ngrams(name.getName())
                .stream()
                .mapToDouble(value -> stats.getOrDefault(value, 0))
                .sum();
    }

    private Map<String, Integer> getNgramStats(User user) throws VoteException, RelationshipException, NameException {
        final var votes = this.votesRepository.all(user);

        final var namesWithVotes = namesRepository
                .all(
                        getNameOwnerUserIds(user),
                        null,
                        0,
                        Integer.MAX_VALUE,
                        null,
                        null,
                        Collections.singleton(new FilterVote(Collections.singleton(user.getId()), FilterVoteCondition.ANY_VOTE))
                )
                .stream()
                .collect(
                        Collectors.toMap(
                                Name::getId,
                                name -> name,
                                (name1, name2) -> name1
                        )
                );

        final var counts = new HashMap<String, Integer>();
        final var voteByName = votes.stream()
                .map(vote -> Map.entry(namesWithVotes.get(vote.getNameId()).getName(), vote.getValue()))
                .filter(entry -> entry.getValue() != 0)
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
