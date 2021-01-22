package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.TestUtil;
import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.repository.users.DbUserRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DbNamesRepositoryTest {

    private static User user;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static DbNamesRepository namesRepository;

    @BeforeAll
    static void beforeAll() throws LiquibaseException, UserException, SQLException {
        jdbcTemplate = TestUtil.createJdbcTemplate();

        final var userRepository = new DbUserRepository(jdbcTemplate, mock(Metrics.class));

        user = userRepository.add();

        namesRepository = new DbNamesRepository(jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM name_attributes_float", Collections.emptyMap());
        jdbcTemplate.update("DELETE FROM name_owners", Collections.emptyMap());
        jdbcTemplate.update("DELETE FROM names", Collections.emptyMap());
    }

    @Test
    void all_oneNameWithoutAttributes() throws NameException {
        namesRepository.add("Alice", user, Collections.emptySet());

        final var actual = new ArrayList<Name>();
        namesRepository.all(null, null, 0, Integer.MAX_VALUE, null, null, null, actual::add);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void all_twoNamesWithoutAttributes() throws NameException {
        namesRepository.add("Alice", user, Collections.emptySet());
        namesRepository.add("Bob", user, Collections.emptySet());

        final var actual = new ArrayList<Name>();
        namesRepository.all(null, null, 0, Integer.MAX_VALUE, null, null, null, actual::add);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
        assertThat(actual.get(1).getName()).isEqualTo("Bob");
    }

    @Test
    void all_findTwoNamesWithoutAttributes() throws NameException {
        namesRepository.add("Adele", user, Collections.emptySet());
        namesRepository.add("Alice", user, Collections.emptySet());
        namesRepository.add("Alicia", user, Collections.emptySet());
        namesRepository.add("Bob", user, Collections.emptySet());

        final var actual = new ArrayList<Name>();
        namesRepository.all(null, "Alic", 0, Integer.MAX_VALUE, null, null, null, actual::add);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
        assertThat(actual.get(1).getName()).isEqualTo("Alicia");
    }

    @Test
    void all_twoMatchingNamesWithAttributes() throws NameException {
        namesRepository.add("Alice", user, Set.of(
                new AttributeNumeric(AttributeKey.SCB_PERCENT_OF_POPULATION, 0.1)
        ));
        namesRepository.add("Alicia", user, Set.of(
                new AttributeNumeric(AttributeKey.SCB_PERCENT_OF_POPULATION, 0.2),
                new AttributeNumeric(AttributeKey.SCB_PERCENT_WOMEN, 0.05)
        ));
        namesRepository.add("AAA Not returned", user, Set.of(
                new AttributeNumeric(AttributeKey.SCB_PERCENT_OF_POPULATION, 0.3)
        ));
        namesRepository.add("ZZZ Not returned", user, Set.of(
                new AttributeNumeric(AttributeKey.SCB_PERCENT_OF_POPULATION, 0.3)
        ));

        final var actual = new ArrayList<Name>();
        namesRepository.all(null, "Alic", 0, Integer.MAX_VALUE, null, null, null, actual::add);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
        assertThat(actual.get(0).getAttributes()).hasSize(2);
        assertThat(actual.get(0).getAttribute(AttributeKey.SYLLABLE_COUNT).map(Attribute::getValue).get()).isEqualTo(3.0);
        assertThat(actual.get(0).getAttribute(AttributeKey.SCB_PERCENT_OF_POPULATION).map(Attribute::getValue).get()).isEqualTo(0.1);

        assertThat(actual.get(1).getName()).isEqualTo("Alicia");
        assertThat(actual.get(1).getAttributes()).hasSize(3);
        assertThat(actual.get(1).getAttribute(AttributeKey.SYLLABLE_COUNT).map(Attribute::getValue).get()).isEqualTo(3.0);
        assertThat(actual.get(1).getAttribute(AttributeKey.SCB_PERCENT_OF_POPULATION).map(Attribute::getValue).get()).isEqualTo(0.2);
        assertThat(actual.get(1).getAttribute(AttributeKey.SCB_PERCENT_WOMEN).map(Attribute::getValue).get()).isEqualTo(0.05);
    }

    @Test
    void all_pagination() throws NameException {
        namesRepository.add("Alice", user, Set.of(
                new AttributeNumeric(AttributeKey.SCB_PERCENT_OF_POPULATION, 0.1)
        ));
        namesRepository.add("Bob 1", user, Collections.emptySet());
        namesRepository.add("Bob 2", user, Set.of(
                new AttributeNumeric(AttributeKey.SCB_PERCENT_OF_POPULATION, 0.2)
        ));
        namesRepository.add("Bob 3", user, Collections.emptySet());
        namesRepository.add("Bob 4", user, Collections.emptySet());
        namesRepository.add("Carol", user, Collections.emptySet());

        final var actual0 = new ArrayList<Name>();
        namesRepository.all(null, "Bob", 0, 2, null, null, null, actual0::add);
        assertThat(actual0).hasSize(2);
        assertThat(actual0.get(0).getName()).isEqualTo("Bob 1");
        assertThat(actual0.get(0).getAttributes()).hasSize(1);
        assertThat(actual0.get(1).getName()).isEqualTo("Bob 2");
        assertThat(actual0.get(1).getAttributes()).hasSize(2);

        final var actual1 = new ArrayList<Name>();
        namesRepository.all(null, "Bob", 1, 2, null, null, null, actual1::add);
        assertThat(actual1).hasSize(2);
        assertThat(actual1.get(0).getName()).isEqualTo("Bob 2");
        assertThat(actual1.get(0).getAttributes()).hasSize(2);
        assertThat(actual1.get(1).getName()).isEqualTo("Bob 3");
        assertThat(actual1.get(1).getAttributes()).hasSize(1);

        final var actual2 = new ArrayList<Name>();
        namesRepository.all(null, "Bob", 2, 2, null, null, null, actual2::add);
        assertThat(actual2).hasSize(2);
        assertThat(actual2.get(0).getName()).isEqualTo("Bob 3");
        assertThat(actual2.get(0).getAttributes()).hasSize(1);
        assertThat(actual2.get(1).getName()).isEqualTo("Bob 4");
        assertThat(actual2.get(1).getAttributes()).hasSize(1);
    }

    @Test
    void all_emptyDatabase() throws NameException {
        final var actual = new ArrayList<Name>();
        namesRepository.all(null, null, 0, Integer.MAX_VALUE, null, null, null, actual::add);
        assertThat(actual).isEmpty();
    }
}