package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.NameBase;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.NameException;
import info.mikaelsvensson.babyname.service.repository.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.UserException;
import info.mikaelsvensson.babyname.service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ScbNameImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScbNameImporter.class);

    public static final String SYSTEM_NAME = "scbImporter";

    @Value("classpath:names_sorted.txt")
    private Resource database;

    @Autowired
    private NamesRepository namesRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    // About event listener: https://www.baeldung.com/running-setup-logic-on-startup-in-spring
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(database.getInputStream(), StandardCharsets.UTF_8))) {
            final var user = getUser();

            final var existingNames = namesRepository.all(Set.of(user.getId()), null, Integer.MAX_VALUE, null).stream()
                    .map(NameBase::getName)
                    .collect(Collectors.toSet());

            reader.lines()
                    .map(line -> Pattern.compile(",").split(line))
                    .filter(columns -> columns.length == 3)
                    .filter(columns -> !existingNames.contains(columns[0]))
                    .map(columns -> new Name(
                            columns[0],
                            !"".equals(columns[1]) ? Integer.parseInt(columns[1]) : null,
                            "m".equals(columns[2]),
                            "f".equals(columns[2]),
                            user.getId(),
                            IdUtils.random(),
                            true
                    ))
                    .collect(Collectors.groupingBy(NameBase::getName))
                    .forEach((s, names) -> {
                        var maleCount = names.get(0).isMale() ? Optional.ofNullable(names.get(0).getCount()).orElse(0) : names.size() > 1 ? Optional.ofNullable(names.get(1).getCount()).orElse(0) : 0;
                        var femaleCount = names.get(0).isFemale() ? Optional.ofNullable(names.get(0).getCount()).orElse(0) : names.size() > 1 ? Optional.ofNullable(names.get(1).getCount()).orElse(0) : 0;

                        var firstName = names.get(0);
                        var secondName = names.size() > 1 ? names.get(1) : null;
                        var ratio = 1.0 * Math.min(maleCount, femaleCount) / Math.max(maleCount, femaleCount);
                        try {
                            namesRepository.add(
                                    firstName.getName(),
                                    ratio > 0.1 || maleCount > femaleCount,
                                    ratio > 0.1 || femaleCount > maleCount,
                                    firstName.isPublic(),
                                    firstName.getOwnerUserId());
                        } catch (NameException e) {
                            LOGGER.warn("Could not add " + firstName.getName());
                        }
                    });
        } catch (IOException | NameException e) {
            LOGGER.error("Error when importing names from SCB file.", e);
        }
    }

    public User getUser() {
        if (user == null) {
            try {
                user = userRepository.getBySystemName(SYSTEM_NAME);
            } catch (UserException e) {
                try {
                    user = userRepository.addSystemUser(SYSTEM_NAME);
                } catch (UserException userException) {
                    throw new RuntimeException(userException);
                }
            }
        }
        return user;
    }
}
