package info.mikaelsvensson.babyname.service.repository.anonymousauthenticator;

import info.mikaelsvensson.babyname.service.TestUtil;
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

class DbAnonymousAuthenticatorRepositoryTest {

    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static DbAnonymousAuthenticatorRepository repository;

    @BeforeAll
    static void beforeAll() throws LiquibaseException, SQLException {
        jdbcTemplate = TestUtil.createJdbcTemplate();

        repository = new DbAnonymousAuthenticatorRepository(jdbcTemplate, mock(Metrics.class));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM anonymousauthenticator_identifiers", Collections.emptyMap());
    }

    @Test
    void exists_missingId_happyPath() throws AnonymousAuthenticatorException {
        assertThat(repository.exists("not-uuid")).isFalse();
    }

//    @Test
//    void logUse() {
//    }

    @Test
    void createAndExists_happyPath() throws AnonymousAuthenticatorException {
        final var id = repository.create();
        assertThat(id).isNotEmpty();
        assertThat(repository.exists(id)).isTrue();
    }

    @Test
    void countAndCount_happyPath() throws AnonymousAuthenticatorException {
        final var countBefore = repository.count();
        repository.create();
        final var countAfter = repository.count();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }
}