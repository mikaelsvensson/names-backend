package info.mikaelsvensson.babyname.service.util.nameprovider;

import info.mikaelsvensson.babyname.service.repository.names.Country;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ScbNameImporter extends AbstractNameImporter {

    public static final int BOOT_ORDER = 20;

    public ScbNameImporter(
            @Autowired NamesRepository namesRepository,
            @Autowired UserRepository userRepository,
            @Value("${scbImporter.onStart:true}") boolean onStart,
            @Value("classpath:names/se/names_sorted.txt") Resource database,
            @Autowired TaskScheduler scheduler) {
        super(SYSTEM_NAME, namesRepository, userRepository);
        this.onStart = onStart;
        this.database = database;
        this.scheduler = scheduler;
    }

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

    private final boolean onStart;

    private final Resource database;

    private final TaskScheduler scheduler;

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

                            final var expectedPercentOfPopulation = 1.0 * (femaleCount + maleCount) / totalPeopleCount;
                            final var expectedPercentWomen = countTotal > 0 ? 1.0 * femaleCount / countTotal : null;

                            addName(firstName.name, expectedPercentOfPopulation, expectedPercentWomen, Country.SWEDEN);

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
}
