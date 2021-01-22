package info.mikaelsvensson.babyname.service.repository.relationships;

import info.mikaelsvensson.babyname.service.TestUtil;
import info.mikaelsvensson.babyname.service.repository.users.DbUserRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.SQLException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DbRelationshipsRepositoryTest {

    private static String userIdAlice;
    private static String userIdBob;
    private static String userIdCarol;
    private static String userIdDave;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static DbRelationshipsRepository relationshipsRepository;
    private static DbUserRepository userRepository;

    @BeforeAll
    static void beforeAll() throws LiquibaseException, UserException, SQLException {
        jdbcTemplate = TestUtil.createJdbcTemplate();

        userRepository = new DbUserRepository(jdbcTemplate, mock(Metrics.class));

        relationshipsRepository = new DbRelationshipsRepository(jdbcTemplate, mock(Metrics.class));

        userIdAlice = userRepository.add().getId();
        userIdBob = userRepository.add().getId();
        userIdCarol = userRepository.add().getId();
        userIdDave = userRepository.add().getId();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM relationships", Collections.emptyMap());
    }

    @Test
    void connect_happyPath() throws UserException, RelationshipException {
        // Arrange
        assertRelatedUser(userIdAlice, null);
        assertRelatedUser(userIdBob, null);
        assertRelatedUser(userIdCarol, null);
        assertRelatedUser(userIdDave, null);

        // Act
        relationshipsRepository.connect(
                userRepository.get(userIdAlice),
                userRepository.get(userIdBob)
        );

        // Assert
        assertRelatedUser(userIdAlice, userIdBob);
        assertRelatedUser(userIdBob, userIdAlice);
        assertRelatedUser(userIdCarol, null);
        assertRelatedUser(userIdDave, null);
    }

    @Test
    void connect_changeConnectedUser() throws UserException, RelationshipException {
        // Arrange
        assertRelatedUser(userIdAlice, null);
        assertRelatedUser(userIdBob, null);
        assertRelatedUser(userIdCarol, null);
        assertRelatedUser(userIdDave, null);

        relationshipsRepository.connect(
                userRepository.get(userIdAlice),
                userRepository.get(userIdBob)
        );

        // Act
        relationshipsRepository.connect(
                userRepository.get(userIdBob),
                userRepository.get(userIdCarol)
        );

        // Assert
        assertRelatedUser(userIdAlice, null);
        assertRelatedUser(userIdBob, userIdCarol);
        assertRelatedUser(userIdCarol, userIdBob);
        assertRelatedUser(userIdDave, null);
    }

    private void assertRelatedUser(String userId, String expectedRelatedUserId) throws UserException {
        assertThat(userRepository.get(userId).getRelatedUserId()).isEqualTo(expectedRelatedUserId);
    }
}