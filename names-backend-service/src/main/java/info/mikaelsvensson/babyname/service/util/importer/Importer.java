package info.mikaelsvensson.babyname.service.util.importer;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.PopulationProperties;
import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.AnonymousAuthenticatorException;
import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.AnonymousAuthenticatorRepository;
import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.FirestoreAnonymousAuthenticatorRepository;
import info.mikaelsvensson.babyname.service.repository.names.Country;
import info.mikaelsvensson.babyname.service.repository.names.FirestoreNamesRepository;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.relationships.FirestoreRelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.users.FirestoreUserRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.repository.votes.FirestoreVotesRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.repository.votes.VotesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class Importer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

    private final RelationshipsRepository relationshipsRepository;
    private final NamesRepository namesRepository;
    private final VotesRepository votesRepository;
    private final AnonymousAuthenticatorRepository authenticatorRepository;
    private final UserRepository userRepository;
//    private final ActionsRepository actionsRepository;

    public Importer(RelationshipsRepository relationshipsRepository, NamesRepository namesRepository, VotesRepository votesRepository, AnonymousAuthenticatorRepository authenticatorRepository, UserRepository userRepository/*, ActionsRepository actionsRepository*/) {
        this.relationshipsRepository = relationshipsRepository;
        this.namesRepository = namesRepository;
        this.votesRepository = votesRepository;
        this.authenticatorRepository = authenticatorRepository;
        this.userRepository = userRepository;
//        this.actionsRepository = actionsRepository;
    }

    public static void main(String[] args) throws VoteException, RelationshipException, NameException, AnonymousAuthenticatorException, IOException {
        new Importer(
                new FirestoreRelationshipsRepository(),
                new FirestoreNamesRepository(),
                new FirestoreVotesRepository(),
                new FirestoreAnonymousAuthenticatorRepository(),
                new FirestoreUserRepository()
        ).doImport(DatabaseSnapshot.readSnapshot(Path.of("names_backup_202205120625.sql")));
    }

    public void doImport(DatabaseSnapshot snapshot) throws IOException, RelationshipException, AnonymousAuthenticatorException, NameException, VoteException {
        // Ignore ACTIONS. Only required for history/audit purposes.
        // Add each ANONYMOUSAUTHENTICATOR_IDENTIFIERS. Only used once. Import mostly for data consistency.
        //   Map imported user's id to exported id.
        // For every USERS:
        //   Ignore users with system_name = null
        //   Map imported user's id to exported id.
        // For every RELATIONSHIPS:
        //   Set the "related to" property for both users
        // For each NAME:
        //   Find NAMES_DEMOGRAPHICS for that name.
        //   Calculate NAMES_METRICS (unless it's automatically calculated when names are added).
        //   Map imported name's id to exported id.
        //   Set "owners array" property on name to ids found in NAME_OWNERS
        // For each VOTES:
        //   Add to global "votes" collection.
        //   Also denormalize on users and names?

        var aaiMap = new HashMap<String, String>();
        for (anonymousauthenticator_identifiers anon : snapshot.anonymousauthenticator_identifiers) {
            var id = authenticatorRepository.create();
            aaiMap.put(anon.id, id);
        }

        var usersProgress = new ProgressLogger(LOGGER, snapshot.names.size(), "Processed %.0f%% of the users");
        var userIdMap = new HashMap<String, User>();
        snapshot.users.stream()
                .filter(users -> !Objects.isNull(users.system_name))
                .forEach(users -> {
                    // Copied from info.mikaelsvensson.babyname.service.repository.users.DbUserRepository.mapper
                    try {
                        final var providerData = Optional.ofNullable(users.system_name).orElse(UserProvider.ANONYMOUS.name().toLowerCase() + ":");
                        final var matcher = Pattern.compile("^((?<key>[a-z]+):)?(?<value>.*)$").matcher(providerData);
                        matcher.find();
                        final var key = Optional
                                .ofNullable(matcher.group("key"))
                                .map(String::toUpperCase)
                                .map(UserProvider::valueOf)
                                .orElse(UserProvider.INTERNAL);
                        final var value = matcher.group("value");
                        User addedUser = userRepository.addFromProvider(
                                key,
                                key == UserProvider.ANONYMOUS ? key.name().toLowerCase() + ':' + aaiMap.get(value) : value
                        );
                        userIdMap.put(users.id, addedUser);
                    } catch (UserException e) {
                        e.printStackTrace();
                    }
                    usersProgress.increment();
                });

//        for (actions action : snapshot.actions) {
//            actionsRepository.add(
//                    userIdMap.get(action.created_by),
//                    ActionType.valueOf(action.action),
//                    action.parameters,
//                    action.status
//            );
//        }

        for (relationships relationship : snapshot.relationships) {
            User userA = userIdMap.get(relationship.user_id);
            User userB = userIdMap.get(relationship.related_user_id);
            if (userA == null || userB == null) {
                System.out.println("Skipping a relationship.");
                continue;
            }
            relationshipsRepository.connect(
                    userA,
                    userB);
        }

        var nameIdMap = new HashMap<String, Name>();
        var namesProgress = new ProgressLogger(LOGGER, snapshot.names.size(), "Processed %.0f%% of the names");
        for (names name : snapshot.names) {
            namesProgress.increment();
            Map<Country, PopulationProperties> populationPropertiesMap = snapshot.names_demographics.stream()
                    .filter(names_demographics -> names_demographics.name_id.equals(name.id))
                    .collect(Collectors.toMap(
                            names_demographics -> names_demographics.country,
                            names_demographics -> new PopulationProperties(names_demographics.percent_of_population, names_demographics.percent_women),
                            (prop1, prop2) -> prop1));

            Set<User> owners = snapshot.name_owners.stream()
                    .filter(name_owner -> name_owner.name_id.equals(name.id))
                    .map(name_owners -> userIdMap.get(name_owners.user_id))
                    .filter(user -> {
                        if (user == null) {
                            LOGGER.info("Name owner is missing.");
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toSet());

            Name addedName = namesRepository.add(name.name, populationPropertiesMap, owners);
            nameIdMap.put(name.id, addedName);
        }

        var votesProgress = new ProgressLogger(LOGGER, snapshot.names.size(), "Processed %.0f%% of the votes");
        for (votes vote : snapshot.votes) {
            User user = userIdMap.get(vote.user_id);
            Name name = nameIdMap.get(vote.name_id);
            votesProgress.increment();
            if (name == null || user == null) {
                LOGGER.info(String.format("Skipping vote for user=%s, name=%s", vote.user_id, vote.name_id));
                continue;
            }
            votesRepository.set(user, name, Long.valueOf(vote.value));
        }
    }
}
