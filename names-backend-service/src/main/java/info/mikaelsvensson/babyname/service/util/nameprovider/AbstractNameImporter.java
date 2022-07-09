package info.mikaelsvensson.babyname.service.util.nameprovider;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.model.name.PopulationProperties;
import info.mikaelsvensson.babyname.service.repository.names.Country;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.metrics.MetricEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;

public abstract class AbstractNameImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNameImporter.class);

    public AbstractNameImporter(String systemUserName, NamesRepository namesRepository, UserRepository userRepository) {
        this.systemUserName = systemUserName;
        this.namesRepository = namesRepository;
        this.userRepository = userRepository;
    }

    private final String systemUserName;

    private final NamesRepository namesRepository;

    private final UserRepository userRepository;

    private User user;

    protected void addName(String n, Double expectedPercentOfPopulation, Double expectedPercentWomen, Country country) {
        try {
            var name = namesRepository.add(n, getUser());
            final var existingProps = Optional.ofNullable(name.getDemographics()).orElse(Collections.emptyMap()).get(country);

            final var actualPercentOfPopulation = Optional.ofNullable(existingProps).map(PopulationProperties::getPercentOfPopulation).orElse(null);
            final var actualPercentWomen = Optional.ofNullable(existingProps).map(PopulationProperties::getPercentWomen).orElse(null);

            final var isPercentOfPopulationCorrect = Optional.ofNullable(expectedPercentOfPopulation).orElse(-1.0).doubleValue() == Optional.ofNullable(actualPercentOfPopulation).orElse(-1.0).doubleValue();
            final var isPercentWomenCorrect = Optional.ofNullable(expectedPercentWomen).orElse(-1.0).doubleValue() == Optional.ofNullable(actualPercentWomen).orElse(-1.0).doubleValue();
            if (!isPercentOfPopulationCorrect || !isPercentWomenCorrect) {
                namesRepository.setDemographicsProperties(name, country, new PopulationProperties(
                        expectedPercentOfPopulation,
                        expectedPercentWomen
                ));
            }
        } catch (NameException e) {
            LOGGER.warn("Could not add " + n + " because of this: " + e.getMessage());
        }
    }

    public User getUser() {
        if (user == null) {
            try {
                user = userRepository.getByProvider(UserProvider.INTERNAL, systemUserName);
            } catch (UserException e) {
                try {
                    user = userRepository.addFromProvider(UserProvider.INTERNAL, systemUserName);
                } catch (UserException userException) {
                    throw new RuntimeException(userException);
                }
            }
        }
        return user;
    }

}
