package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.TestUtil;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.name.MetricsProperties;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.PopulationProperties;
import info.mikaelsvensson.babyname.service.repository.names.request.BasicNameFacet;
import info.mikaelsvensson.babyname.service.repository.names.request.MetricsNameFacet;
import info.mikaelsvensson.babyname.service.repository.names.request.NamesRequest;
import info.mikaelsvensson.babyname.service.repository.names.request.PopulationNameFacet;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DbNamesRepositoryTest {

    private static User user1;
    private static User user2;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static DbNamesRepository namesRepository;
    private static ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);

    @BeforeAll
    static void beforeAll() throws LiquibaseException, UserException, SQLException {
        jdbcTemplate = TestUtil.createJdbcTemplate();

        final var userRepository = new DbUserRepository(jdbcTemplate, mock(Metrics.class));

        user1 = userRepository.add();
        user2 = userRepository.add();

        namesRepository = new DbNamesRepository(applicationEventPublisher, jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM name_owners", Collections.emptyMap());
        jdbcTemplate.update("DELETE FROM names", Collections.emptyMap());
    }

    @Test
    void all_oneNameWithoutAttributes() throws NameException {
        namesRepository.add("Alice", user1);

        final var actual = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet()), actual::add);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void all_twoNamesWithMetrics() throws NameException {
        final var nameAlice = namesRepository.add("Alice", user1);
        final var nameBob = namesRepository.add("Bob", user1);
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
        namesRepository.add("Adele", user1);
        namesRepository.add("Alice", user1);
        namesRepository.add("Alicia", user1);
        namesRepository.add("Bob", user1);

        final var actual = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet().namePrefix("Alic")), actual::add);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
        assertThat(actual.get(1).getName()).isEqualTo("Alicia");
    }

    @Test
    void all_findTwoNamesWithDifferentFacets() throws NameException {
        // Arrange
        namesRepository.add("Adele", user1); // <-- should not be returned
        namesRepository.add("Bob", user1); // <-- should not be returned
        final var nameAlice = namesRepository.add("Alice", user1);
        final var nameAlicia = namesRepository.add("Alicia", user1);
        namesRepository.setMetricsProperties(nameAlice, new MetricsProperties(1.0));
        namesRepository.setDemographicsProperties(nameAlicia, Country.SWEDEN, new PopulationProperties(0.001, 0.5));
        namesRepository.setDemographicsProperties(nameAlicia, Country.USA, new PopulationProperties(0.002, 0.6));

        // Act
        final var actual = new ArrayList<Name>();
        namesRepository.all(
                new NamesRequest()
                        .basic(new BasicNameFacet().namePrefix("Alic"))
                        .metrics(new MetricsNameFacet())
                        .demographics(new PopulationNameFacet(), Country.SWEDEN),
                actual::add);

        // Assert
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getName()).isEqualTo("Alice");
        assertThat(actual.get(0).getMetrics().getSyllableCount()).isEqualTo(1.0);
        assertThat(actual.get(0).getDemographics().get(Country.SWEDEN).getPercentOfPopulation()).isNull(); // Data requested but none exist for this name
        assertThat(actual.get(0).getDemographics().get(Country.SWEDEN).getPercentWomen()).isNull(); // Data requested but none exist for this name
        assertThat(actual.get(0).getDemographics().get(Country.USA)).isNull(); // Demographics data for USA not requested
        assertThat(actual.get(1).getName()).isEqualTo("Alicia");
        assertThat(actual.get(1).getMetrics().getSyllableCount()).isNull();
        assertThat(actual.get(1).getDemographics().get(Country.SWEDEN).getPercentOfPopulation()).isEqualTo(0.001);
        assertThat(actual.get(1).getDemographics().get(Country.SWEDEN).getPercentWomen()).isEqualTo(0.5);
        assertThat(actual.get(1).getDemographics().get(Country.USA)).isNull(); // Demographics data for USA not requested

        // Act
        final var actual2 = new ArrayList<Name>();
        namesRepository.all(
                new NamesRequest()
                        .basic(new BasicNameFacet().namePrefix("Alic"))
                        .metrics(new MetricsNameFacet())
                        .demographics(new PopulationNameFacet(), Country.SWEDEN)
                        .demographics(new PopulationNameFacet(), Country.USA),
                actual2::add);

        // Assert
        assertThat(actual2).hasSize(2);
        assertThat(actual2.get(0).getName()).isEqualTo("Alice");
        assertThat(actual2.get(0).getDemographics().get(Country.SWEDEN).getPercentOfPopulation()).isNull();
        assertThat(actual2.get(0).getDemographics().get(Country.SWEDEN).getPercentWomen()).isNull();
        assertThat(actual2.get(0).getDemographics().get(Country.USA).getPercentOfPopulation()).isNull(); // Data requested but none exist for this name
        assertThat(actual2.get(0).getDemographics().get(Country.USA).getPercentWomen()).isNull(); // Data requested but none exist for this name
        assertThat(actual2.get(1).getName()).isEqualTo("Alicia");
        assertThat(actual2.get(1).getDemographics().get(Country.SWEDEN).getPercentOfPopulation()).isEqualTo(0.001);
        assertThat(actual2.get(1).getDemographics().get(Country.SWEDEN).getPercentWomen()).isEqualTo(0.5);
        assertThat(actual2.get(1).getDemographics().get(Country.USA).getPercentOfPopulation()).isEqualTo(0.002);
        assertThat(actual2.get(1).getDemographics().get(Country.USA).getPercentWomen()).isEqualTo(0.6);
    }

    @Test
    void all_pagination() throws NameException {
        namesRepository.add("Alice", user1);
        namesRepository.add("Bob 1", user1);
        namesRepository.add("Bob 2", user1);
        namesRepository.add("Bob 3", user1);
        namesRepository.add("Bob 4", user1);
        namesRepository.add("Carol", user1);

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

    @Test
    void add_simple() throws NameException {
        final var result = namesRepository.add("Alice", user1);

        final var actualGet = namesRepository.getByName("Alice");
        assertThat(actualGet.isPresent()).isTrue();
        assertThat(actualGet.get().getName()).isEqualTo("Alice");
        assertThat(actualGet.get().getName()).isEqualTo(result.getName());
        assertThat(actualGet.get().getId()).isEqualTo(result.getId());
    }

    @Test
    void add_twoOwners() throws NameException {
        final var result1 = namesRepository.add("Alice", user1);
        final var result2 = namesRepository.add("Alice", user2);

        assertThat(result1.getId()).isEqualTo(result2.getId());
        assertThat(result1.getName()).isEqualTo("Alice");
        assertThat(result1.getName()).isEqualTo(result2.getName());
    }

    @Test
    void addAndAll_twoOwners() throws NameException {
        // ARRANGE names owned by user 1
        namesRepository.add("Alice", user1);
        namesRepository.add("Alicia", user1);

        // ARRANGE names owned by user 2
        namesRepository.add("Alice", user2);
        namesRepository.add("Alan", user2);

        // ASSERT
        final var searchResult1 = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet().nameOwnerUserIds(Set.of(user1.getId()))), searchResult1::add);
        assertThat(searchResult1.stream().map(Name::getName).sorted().collect(Collectors.joining(","))).isEqualTo("Alice,Alicia");

        // ASSERT
        final var searchResult2 = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet().nameOwnerUserIds(Set.of(user1.getId(), user2.getId()))), searchResult2::add);
        assertThat(searchResult2.stream().map(Name::getName).sorted().collect(Collectors.joining(","))).isEqualTo("Alan,Alice,Alicia");

        // ASSERT
        final var searchResult3 = new ArrayList<Name>();
        namesRepository.all(new NamesRequest().basic(new BasicNameFacet().nameOwnerUserIds(Set.of(user2.getId()))), searchResult3::add);
        assertThat(searchResult3.stream().map(Name::getName).sorted().collect(Collectors.joining(","))).isEqualTo("Alan,Alice");
    }
}