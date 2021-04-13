package info.mikaelsvensson.babyname.service.repository.users;

import info.mikaelsvensson.babyname.service.TestUtil;
import info.mikaelsvensson.babyname.service.model.ActionStatus;
import info.mikaelsvensson.babyname.service.model.ActionType;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.actions.ActionException;
import info.mikaelsvensson.babyname.service.repository.actions.DbActionsRepository;
import info.mikaelsvensson.babyname.service.repository.names.DbNamesRepository;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.relationships.DbRelationshipsRepository;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.votes.DbVotesRepository;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DbUserRepositoryTest {

    private static User user1;
    private static User user2;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static DbUserRepository userRepository;
    private static DbNamesRepository namesRepository;
    private static DbVotesRepository votesRepository;
    private static DbRelationshipsRepository relationshipsRepository;
    private static DbActionsRepository actionsRepository;

    @BeforeAll
    static void beforeAll() throws LiquibaseException, UserException, SQLException {
        jdbcTemplate = TestUtil.createJdbcTemplate();

        userRepository = new DbUserRepository(jdbcTemplate, mock(Metrics.class));

        user1 = userRepository.add();
        user2 = userRepository.add();

        namesRepository = new DbNamesRepository(mock(ApplicationEventPublisher.class), jdbcTemplate);
        votesRepository = new DbVotesRepository(jdbcTemplate, mock(Metrics.class));
        relationshipsRepository = new DbRelationshipsRepository(jdbcTemplate, mock(Metrics.class));
        actionsRepository = new DbActionsRepository(jdbcTemplate);
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void delete() throws UserException, NameException, VoteException, RelationshipException, ActionException {
        // ARRANGE
        final var user3 = userRepository.add();
        final var user4 = userRepository.add();

        final var nameAlice = namesRepository.add("Alice", user3);
        final var nameBob = namesRepository.add("Bob", user3);

        votesRepository.set(user3, nameAlice, 100L);
        votesRepository.set(user3, nameBob, -100L);

        votesRepository.set(user4, nameBob, -100L);

        relationshipsRepository.connect(user3, user4);

        actionsRepository.add(user3, ActionType.VERIFY_EMAIL, Map.of("key", "value"), ActionStatus.PENDING);

        final Function<String, Integer> tableRowCount = (String table) -> jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, new MapSqlParameterSource(), Integer.class);

        var userCountBefore = tableRowCount.apply("users");
        var voteCountBefore = tableRowCount.apply("votes");
        var nameCountBefore = tableRowCount.apply("names");
        var relationshipCountBefore = tableRowCount.apply("relationships");
        var actionCountBefore = tableRowCount.apply("actions");

        // ACT
        userRepository.delete(user3);

        // ASSERT
        assertThat(tableRowCount.apply("users")).isEqualTo(userCountBefore - 1);
        assertThat(tableRowCount.apply("votes")).isEqualTo(voteCountBefore - 2);
        assertThat(tableRowCount.apply("names")).isEqualTo(nameCountBefore);
        assertThat(tableRowCount.apply("relationships")).isEqualTo(relationshipCountBefore - 2);
        assertThat(tableRowCount.apply("actions")).isEqualTo(actionCountBefore - 1);
    }
}
