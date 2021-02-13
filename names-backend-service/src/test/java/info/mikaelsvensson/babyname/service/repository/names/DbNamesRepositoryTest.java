package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.TestUtil;
import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.model.name.MetricsProperties;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.ScbProperties;
import info.mikaelsvensson.babyname.service.repository.names.request.BasicNameFacet;
import info.mikaelsvensson.babyname.service.repository.names.request.MetricsNameFacet;
import info.mikaelsvensson.babyname.service.repository.names.request.NamesRequest;
import info.mikaelsvensson.babyname.service.repository.names.request.ScbNameFacet;
import info.mikaelsvensson.babyname.service.repository.users.DbUserRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DbNamesRepositoryTest {

    private static User user;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static DbNamesRepository namesRepository;
    private static ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);

    @BeforeAll
    static void beforeAll() throws LiquibaseException, UserException, SQLException {
        jdbcTemplate = TestUtil.createJdbcTemplate();

        final var userRepository = new DbUserRepository(jdbcTemplate, mock(Metrics.class));

        user = userRepository.add();

        namesRepository = new DbNamesRepository(applicationEventPublisher, jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM name_owners", Collections.emptyMap());
        jdbcTemplate.update("DELETE FROM names", Collections.emptyMap());
    }

    @Test
    void all_oneNameWithoutAttributes() throws NameException {
        namesRepository.add("Alice", user);

        final var actual = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet()), actual::add);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void all_twoNamesWithMetrics() throws NameException {
        final var nameAlice = namesRepository.add("Alice", user);
        final var nameBob = namesRepository.add("Bob", user);
        namesRepository.setMetricsProperties(nameAlice, new MetricsProperties(1.0));
        namesRepository.setMetricsProperties(nameBob, new MetricsProperties(2.0));

        final var actual = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet()).metrics(new MetricsNameFacet()), actual::add);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
        assertThat(actual.get(0).getMetrics().getSyllableCount()).isEqualTo(1);
        assertThat(actual.get(1).getName()).isEqualTo("Bob");
        assertThat(actual.get(1).getMetrics().getSyllableCount()).isEqualTo(2);
    }

    @Test
    void all_findTwoNames() throws NameException {
        namesRepository.add("Adele", user);
        namesRepository.add("Alice", user);
        namesRepository.add("Alicia", user);
        namesRepository.add("Bob", user);

        final var actual = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet().namePrefix("Alic")), actual::add);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
        assertThat(actual.get(1).getName()).isEqualTo("Alicia");
    }

    @Test
    void all_findTwoNamesWithDifferentFacets() throws NameException {
        namesRepository.add("Adele", user); // <-- should not be returned
        namesRepository.add("Bob", user); // <-- should not be returned
        final var nameAlice = namesRepository.add("Alice", user);
        final var nameAlicia = namesRepository.add("Alicia", user);
        namesRepository.setMetricsProperties(nameAlice, new MetricsProperties(1.0));
        namesRepository.setScbProperties(nameAlicia, new ScbProperties(0.001, 0.5));

        final var actual = new ArrayList<Name>();
        namesRepository.all(
                new NamesRequest()
                        .basic(new BasicNameFacet().namePrefix("Alic"))
                        .metrics(new MetricsNameFacet())
                        .scb(new ScbNameFacet()),
                actual::add);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
        assertThat(actual.get(0).getMetrics().getSyllableCount()).isEqualTo(1.0);
        assertThat(actual.get(0).getScb().getPercentOfPopulation()).isNull();
        assertThat(actual.get(0).getScb().getPercentWomen()).isNull();
        assertThat(actual.get(1).getName()).isEqualTo("Alicia");
        assertThat(actual.get(1).getMetrics().getSyllableCount()).isNull();
        assertThat(actual.get(1).getScb().getPercentOfPopulation()).isEqualTo(0.001);
        assertThat(actual.get(1).getScb().getPercentWomen()).isEqualTo(0.5);
    }

    @Test
    void all_pagination() throws NameException {
        namesRepository.add("Alice", user);
        namesRepository.add("Bob 1", user);
        namesRepository.add("Bob 2", user);
        namesRepository.add("Bob 3", user);
        namesRepository.add("Bob 4", user);
        namesRepository.add("Carol", user);

        final var actual0 = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet().namePrefix("Bob")).offset(0).limit(2), actual0::add);
        assertThat(actual0).hasSize(2);
        assertThat(actual0.get(0).getName()).isEqualTo("Bob 1");
        assertThat(actual0.get(1).getName()).isEqualTo("Bob 2");

        final var actual1 = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet().namePrefix("Bob")).offset(1).limit(2), actual1::add);
        assertThat(actual1).hasSize(2);
        assertThat(actual1.get(0).getName()).isEqualTo("Bob 2");
        assertThat(actual1.get(1).getName()).isEqualTo("Bob 3");

        final var actual2 = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet().namePrefix("Bob")).offset(2).limit(2), actual2::add);
        assertThat(actual2).hasSize(2);
        assertThat(actual2.get(0).getName()).isEqualTo("Bob 3");
        assertThat(actual2.get(1).getName()).isEqualTo("Bob 4");
    }

    @Test
    void all_emptyDatabase() throws NameException {
        final var actual = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet()), actual::add);
        assertThat(actual).isEmpty();
    }
}