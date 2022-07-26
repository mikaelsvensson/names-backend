package info.mikaelsvensson.babyname.service.repository.names;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.common.collect.ImmutableMap;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.model.name.MetricsProperties;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.PopulationProperties;
import info.mikaelsvensson.babyname.service.model.name.VotesProperties;
import info.mikaelsvensson.babyname.service.repository.Datastore;
import info.mikaelsvensson.babyname.service.repository.names.request.*;
import info.mikaelsvensson.babyname.service.repository.votes.FirestoreVotesRepository;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import info.mikaelsvensson.babyname.service.util.NameFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@Service
public class FirestoreNamesRepository implements NamesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirestoreNamesRepository.class);
    private final Firestore db;
    private final FirestoreVotesRepository votesRepository;

    public FirestoreNamesRepository() {
        db = Datastore.get();
        votesRepository = new FirestoreVotesRepository();
    }

    @Override
    public void all(NamesRequest request, Consumer<Name> nameConsumer) throws NameException {
        find(request, nameConsumer);
    }

    static enum Gender {
        MAN,
        WOMAN,
        UNISEX;
    }

    static enum Popularity {
        HIGH,
        MEDIUM,
        LOW
    }

    static enum Length {
        SHORT,
        MEDIUM,
        LONG
    }

    private void find(NamesRequest request, Consumer<Name> nameConsumer) throws NameException {
        try {
            if (request.offset > 0) {
                LOGGER.warn("Inefficient query. Query result offset is set to " + request.offset);
            }

            final var orderByType = Optional.ofNullable(request.sortOrder).orElse(SortOrder.NAME);
            final var orderBy = switch (orderByType) {
                case NAME -> "name";
                case RANDOM -> throw new NameException("Unsupported sort order: " + orderByType);
            };
            Query query = db.collection("names")
                    .orderBy(orderBy)
                    .limit(request.limit)
                    .offset(request.offset);

            if (request.basic != null && request.basic.nameId != null) {
                throw new NameException("Unsupported filter: nameId");
            }
            if (request.basic != null && request.basic.nameExact != null) {
                query = query.whereEqualTo("name", request.basic.nameExact);
            }
            if (request.basic != null && request.basic.namePrefix != null) {
                query = query.whereLessThanOrEqualTo("name", request.basic.namePrefix.toLowerCase())
                        .whereGreaterThanOrEqualTo("name", request.basic.namePrefix.toLowerCase() + '\uf8ff');
            }
            if (request.basic != null && request.basic.nameOwnerUserIds != null && !request.basic.nameOwnerUserIds.isEmpty()) {
                query = query.whereArrayContainsAny("owner_user_ids", Arrays.asList(request.basic.nameOwnerUserIds.toArray()));
            }
            if (request.metrics != null && !request.metrics.syllableFilter.isEmpty()) {
                final var syllableLowLimit = request.metrics.syllableFilter.stream().filter(filterNumeric -> filterNumeric.getOperator() == FilterNumericOperator.GREATER_THAN).mapToDouble(FilterNumeric::getValue).min().orElse(0.0);
                final var syllableHighLimit = request.metrics.syllableFilter.stream().filter(filterNumeric -> filterNumeric.getOperator() == FilterNumericOperator.LESS_THAN).mapToDouble(FilterNumeric::getValue).max().orElse(100.0);
                if (syllableLowLimit > 1 && syllableHighLimit < 4) {
                    query = query.whereEqualTo(FieldPath.of("metrics", "length"), Length.MEDIUM.name().toLowerCase());
                } else if (syllableHighLimit > 3) {
                    query = query.whereEqualTo(FieldPath.of("metrics", "length"), Length.LONG.name().toLowerCase());
                } else if (syllableLowLimit < 2) {
                    query = query.whereEqualTo(FieldPath.of("metrics", "length"), Length.SHORT.name().toLowerCase());
                }
            }
            if (request.demographics != null) {
                for (Map.Entry<Country, PopulationNameFacet> entry : request.demographics.entrySet()) {
                    final var facet = entry.getValue();
                    final var countryCode = entry.getKey().getCountryCode();

                    final var isAtLeastThisPopularFilterDefined = facet.percentOfPopulationFilter.stream().anyMatch(filterNumeric -> filterNumeric.getOperator() == FilterNumericOperator.GREATER_THAN);
                    final var isAtMostThisPopularFilterDefined = facet.percentOfPopulationFilter.stream().anyMatch(filterNumeric -> filterNumeric.getOperator() == FilterNumericOperator.LESS_THAN);
                    if (isAtLeastThisPopularFilterDefined && isAtMostThisPopularFilterDefined) {
                        query = query.whereEqualTo(FieldPath.of("demographics", countryCode, "popularity"), Popularity.MEDIUM.name().toLowerCase());
                    } else if (isAtLeastThisPopularFilterDefined) {
                        query = query.whereEqualTo(FieldPath.of("demographics", countryCode, "popularity"), Popularity.HIGH.name().toLowerCase());
                    } else if (isAtMostThisPopularFilterDefined) {
                        query = query.whereEqualTo(FieldPath.of("demographics", countryCode, "popularity"), Popularity.LOW.name().toLowerCase());
                    }

                    final var isAtLeastThisPercentWomenFilterDefined = facet.percentWomenFilter.stream().anyMatch(filterNumeric -> filterNumeric.getOperator() == FilterNumericOperator.GREATER_THAN);
                    final var isAtMostThisPercentWomenFilterDefined = facet.percentWomenFilter.stream().anyMatch(filterNumeric -> filterNumeric.getOperator() == FilterNumericOperator.LESS_THAN);
                    if (isAtLeastThisPercentWomenFilterDefined && isAtMostThisPercentWomenFilterDefined) {
                        query = query.whereEqualTo(FieldPath.of("demographics", countryCode, "gender"), Gender.UNISEX.name().toLowerCase());
                    } else if (isAtLeastThisPercentWomenFilterDefined) {
                        query = query.whereEqualTo(FieldPath.of("demographics", countryCode, "gender"), Gender.WOMAN.name().toLowerCase());
                    } else if (isAtMostThisPercentWomenFilterDefined) {
                        query = query.whereEqualTo(FieldPath.of("demographics", countryCode, "gender"), Gender.MAN.name().toLowerCase());
                    }
                }
            }

            if (request.votes != null && request.votes.filterVotes != null && !request.votes.filterVotes.isEmpty()) {
                if (request.votes.filterVotes.size() > 1) {
                    throw new NameException("Unsupported filter: multiple vote conditions");
                }
                var filterVote = request.votes.filterVotes.iterator().next();
                final var votes = votesRepository.all(filterVote);
                final var nameIds = votes.stream()
                        .map(Vote::getNameId)
                        .map(nameId -> db.collection("names").document(nameId))
                        .collect(Collectors.toUnmodifiableSet());

                final Map<String, Long> selfVotes = request.votes.returned && request.votes.selfUserId != null ? votes.stream().filter(vote -> vote.getUserId().equals(request.votes.selfUserId)).collect(Collectors.toMap(Vote::getNameId, Vote::getValue)) : Collections.emptyMap();
                final Map<String, Long> partnerVotes = request.votes.returned && request.votes.partnerUserId != null ? votes.stream().filter(vote -> vote.getUserId().equals(request.votes.partnerUserId)).collect(Collectors.toMap(Vote::getNameId, Vote::getValue)) : Collections.emptyMap();
                for (DocumentSnapshot nameSnapshot : db.getAll(nameIds.toArray(new DocumentReference[0])).get()) {
                    final var name = toName(nameSnapshot, selfVotes, partnerVotes);
                    nameConsumer.accept(name);
                }
                return;
            }

            for (QueryDocumentSnapshot nameSnapshot : query.get().get().getDocuments()) {
                nameConsumer.accept(toName(nameSnapshot, Collections.emptyMap(), Collections.emptyMap()));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to query database.", e);
            throw new NameException(e.getMessage());
        }
    }

    private Name toName(DocumentSnapshot nameSnapshot, Map<String, Long> selfVotes, Map<String, Long> partnerVotes) {
        var name = new Name(nameSnapshot.get("name", String.class), nameSnapshot.getId());

        Double genderValue = nameSnapshot.get(FieldPath.of("demographics", Country.SWEDEN.getCountryCode(), "percentWomen"), Double.class);
        Double popularityValue = nameSnapshot.get(FieldPath.of("demographics", Country.SWEDEN.getCountryCode(), "percentOfPopulation"), Double.class);
        if (genderValue != null && popularityValue != null) {
            name.setDemographics(Collections.singletonMap(Country.SWEDEN, new PopulationProperties(popularityValue, genderValue)));
        } else {
            name.setDemographics(Collections.singletonMap(Country.SWEDEN, new PopulationProperties(null, null)));
        }
        if (!selfVotes.isEmpty() || !partnerVotes.isEmpty()) {
            name.setVotes(new VotesProperties(
                    selfVotes.get(name.getId()),
                    partnerVotes.get(name.getId())
            ));
        }
        return name;
    }


    @Override
    public synchronized Map<String, String> allNames() throws NameException {
        var names = new HashMap<String, String>();
        db.collection("names").listDocuments().forEach(documentReference -> {
            try {
                names.put(documentReference.getId(), documentReference.get().get().getString("name"));
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(e.getMessage());
            }
        });
        return names;
    }

    @Override
    public synchronized Name add(String name, User user) throws NameException {
        return add(name, null, Collections.singleton(user));
    }

    @Override
    public Name add(String name, Map<Country, PopulationProperties> demographics, Set<User> users) throws NameException {
        var newName = new Name(name, IdUtils.random());
        newName.setDemographics(demographics);

        HashMap<String, Object> props = new HashMap<>();
        props.put("name", newName.getName());
        HashMap<String, Map<String, Object>> demographicsMap = demographics.entrySet().stream().collect(
                HashMap::new,
                (acc, keyAndValue) -> {
                    String countryCode = keyAndValue.getKey().getCountryCode();
                    PopulationProperties properties = keyAndValue.getValue();
                    acc.put(countryCode, getDemographicsMap(properties));
                },
                HashMap::putAll);
        props.put("demographics", demographicsMap);
        props.put("metrics", getMetricsMap(getMetrics(name)));
        props.put("owner_user_ids", users.stream().map(User::getId).collect(Collectors.toList()));
        db.collection("names").document(newName.getId()).set(props);
        return newName;
    }

    @Override
    public Name get(String nameId, User user) throws NameException {
        try {
            return toName(db.collection("names").document(nameId).get().get(), Collections.emptyMap(), Collections.emptyMap());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not get name " + nameId);
            throw new NameException(e.getMessage());
        }
    }

    @Override
    public Optional<Name> getByName(String name) throws NameException {
        try {
            List<QueryDocumentSnapshot> querySnapshot = db.collection("names").whereEqualTo("name", name).get().get().getDocuments();
            if (querySnapshot.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(toName(querySnapshot.get(0), Collections.emptyMap(), Collections.emptyMap()));
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not find name " + name);
            throw new NameException(e.getMessage());
        }
    }

    @Override
    public void setDemographicsProperties(Name name, Country country, PopulationProperties properties) throws NameException {
        db.collection("names").document(name.getId()).update(FieldPath.of("demographics", country.getCountryCode()), getDemographicsMap(properties));
    }

    @Override
    public void setMetricsProperties(Name name, MetricsProperties properties) throws NameException {
        db.collection("names").document(name.getId()).update("metrics", getMetricsMap(getMetrics(name.getName())));
    }

    private MetricsProperties getMetrics(String name) {
        return new MetricsProperties((double) NameFeature.syllableCount(name));
    }

    private Map<String, Object> getMetricsMap(MetricsProperties properties) {
        Double syllableCount = properties.getSyllableCount();
        var length = (syllableCount > 1 && syllableCount < 4) ?
                Length.MEDIUM :
                (syllableCount > 3) ?
                        Length.LONG : Length.SHORT;

        return ImmutableMap.of(
                "syllableCount", syllableCount,
                "length", length.name().toLowerCase());
    }

    private Map<String, Object> getDemographicsMap(PopulationProperties properties) {
        HashMap<String, Object> props = new HashMap<>();
        if (properties.getPercentOfPopulation() != null) {
            props.put("percentOfPopulation", properties.getPercentOfPopulation());
            if (properties.getPercentOfPopulation() > 0.00001 && properties.getPercentOfPopulation() < 0.001) {
                props.put("popularity", Popularity.MEDIUM.name().toLowerCase());
            } else if (properties.getPercentOfPopulation() > 0.001) {
                props.put("popularity", Popularity.HIGH.name().toLowerCase());
            } else if (properties.getPercentOfPopulation() < 0.00001) {
                props.put("popularity", Popularity.LOW.name().toLowerCase());
            }
        }
        if (properties.getPercentWomen() != null) {
            props.put("percentWomen", properties.getPercentWomen());
            if (properties.getPercentWomen() > 0.1 && properties.getPercentWomen() < 0.9) {
                props.put("gender", Gender.UNISEX.name().toLowerCase());
            } else if (properties.getPercentWomen() > 0.9) {
                props.put("gender", Gender.WOMAN.name().toLowerCase());
            } else if (properties.getPercentWomen() < 0.1) {
                props.put("gender", Gender.MAN.name().toLowerCase());
            }
        }

        return props;
    }
}
