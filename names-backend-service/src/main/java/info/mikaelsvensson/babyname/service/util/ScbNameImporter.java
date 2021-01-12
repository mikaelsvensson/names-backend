package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ScbNameImporter {

    public static final int BOOT_ORDER = 20;

    private static class FileEntry {
        private final String name;
        private final boolean isMale;
        private final boolean isFemale;
        private final long count;

        public FileEntry(String name, boolean isMale, boolean isFemale, long count) {
            this.name = name;
            this.isMale = isMale;
            this.isFemale = isFemale;
            this.count = count;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ScbNameImporter.class);

    public static final String SYSTEM_NAME = "scbImporter";

    @Value("${scbImporter.onStart:true}")
    private boolean onStart;

    @Value("classpath:names_sorted.txt")
    private Resource database;

    @Autowired
    private NamesRepository namesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskScheduler scheduler;

    private User user;

    // About event listener: https://www.baeldung.com/running-setup-logic-on-startup-in-spring
    @EventListener
    @Order(BOOT_ORDER)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!onStart) {
            LOGGER.info("SCB data sync skipped.");
            return;
        }
        scheduler.schedule(() -> {
            LOGGER.info("SCB data sync started.");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(database.getInputStream(), StandardCharsets.UTF_8))) {
                final var user = getUser();

                final var fileEntries = reader.lines()
                        .map(line -> Pattern.compile(",").split(line))
                        .filter(columns -> columns.length == 3)
                        .map(columns -> new FileEntry(
                                columns[0],
                                "m".equals(columns[2]),
                                "f".equals(columns[2]),
                                !"".equals(columns[1]) ? Long.parseLong(columns[1]) : 0
                        ))
                        .collect(Collectors.toSet());

                LOGGER.info("{} entries in raw data.", fileEntries.size());

                final var totalPeopleCount = fileEntries.stream().mapToLong(fileEntry -> fileEntry.count).sum();

                final var gcCounter = new MutableInt(0);

                fileEntries.stream()
                        .collect(Collectors.groupingBy(fileEntry -> fileEntry.name))
                        .forEach((s, entries) -> {
                            var maleCount = entries.get(0).isMale ? entries.get(0).count : entries.size() > 1 ? entries.get(1).count : 0;
                            var femaleCount = entries.get(0).isFemale ? entries.get(0).count : entries.size() > 1 ? entries.get(1).count : 0;

                            var firstName = entries.get(0);
                            final var countTotal = femaleCount + maleCount;

                            try {
                                var nameRes = namesRepository.getByName(firstName.name);
                                var name = nameRes.isEmpty() ? namesRepository.add(firstName.name, user, Collections.emptySet()) : nameRes.get();

                                final var expectedAttrs = new HashMap<AttributeKey, Double>();
                                expectedAttrs.put(
                                        AttributeKey.SCB_PERCENT_OF_POPULATION,
                                        1.0 * (femaleCount + maleCount) / totalPeopleCount);
                                expectedAttrs.put(
                                        AttributeKey.SCB_PERCENT_WOMEN,
                                        countTotal > 0 ? 1.0 * femaleCount / countTotal : null);
                                for (Map.Entry<AttributeKey, Double> entry : expectedAttrs.entrySet()) {
                                    final var key = entry.getKey();
                                    final var expectedValue = entry.getValue();
                                    final var actualValue = name.getAttribute(key).map(Attribute::getValue).orElse(null);
                                    if (actualValue != null || expectedValue != null) {
                                        // We have a value OR should have a value
                                        if (actualValue == null) {
                                            // We don't have a value but we should. Add it.
                                            namesRepository.setNumericAttribute(name, user, key, expectedValue);
                                        } else if (expectedValue == null) {
                                            // We have a value but shouldn't. Unset it.
                                            namesRepository.setNumericAttribute(name, user, key, expectedValue);
                                        } else if (!actualValue.equals(expectedValue)) {
                                            // We have a value but should have a different. Change it.
                                            namesRepository.setNumericAttribute(name, user, key, expectedValue);
                                        }
                                    }
                                }
                            } catch (NameException e) {
                                LOGGER.warn("Could not add " + firstName.name + " because of this: " + e.getMessage());
                            }

                            gcCounter.increment();
                            if (gcCounter.intValue() % 1000 == 0) {
                                LOGGER.info("{} names processed. Time for garbage collection.", gcCounter.intValue());
                                System.gc();
                            }
                        });

                System.gc();
            } catch (IOException e) {
                LOGGER.error("Error when importing names from SCB file.", e);
            }
            LOGGER.info("SCB data sync done.");
        }, Instant.now().plusSeconds(1));
    }

    public User getUser() {
        if (user == null) {
            try {
                user = userRepository.getByProvider(UserProvider.INTERNAL, SYSTEM_NAME);
            } catch (UserException e) {
                try {
                    user = userRepository.addFromProvider(UserProvider.INTERNAL, SYSTEM_NAME);
                } catch (UserException userException) {
                    throw new RuntimeException(userException);
                }
            }
        }
        return user;
    }
}
