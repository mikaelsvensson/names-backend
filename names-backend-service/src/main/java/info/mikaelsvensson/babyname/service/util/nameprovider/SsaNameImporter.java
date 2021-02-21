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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SsaNameImporter extends AbstractNameImporter {

    public static final int BOOT_ORDER = 20;

    public SsaNameImporter(
            @Autowired NamesRepository namesRepository,
            @Autowired UserRepository userRepository,
            @Value("${ssaImporter.onStart:true}") boolean onStart,
            @Value("classpath:names/us/*.csv") Resource[] databases,
            @Autowired TaskScheduler scheduler) {
        super(SYSTEM_NAME, namesRepository, userRepository);
        this.onStart = onStart;
        this.databases = databases;
        this.scheduler = scheduler;
    }

    private static class FileEntry {
        private final String name;
        private final boolean isFemale;
        private final double percentage;

        public FileEntry(String name, boolean isFemale, double percentage) {
            this.name = name;
            this.isFemale = isFemale;
            this.percentage = percentage;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SsaNameImporter.class);

    public static final String SYSTEM_NAME = "ssaImporter";

    private final boolean onStart;

    private final Resource[] databases;

    private final TaskScheduler scheduler;

    // About event listener: https://www.baeldung.com/running-setup-logic-on-startup-in-spring
    @EventListener
    @Order(BOOT_ORDER)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!onStart) {
            LOGGER.info("SSA data sync skipped.");
            return;
        }
        scheduler.schedule(() -> {
            LOGGER.info("SSA data sync started.");
            final var fileEntries = new ArrayList<FileEntry>();
            for (Resource database : databases) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(database.getInputStream(), StandardCharsets.UTF_8))) {

                    reader.lines()
                            .limit(2 * 100)
                            .map(line -> Pattern.compile(",").split(line))
                            .filter(columns -> columns.length == 3)
                            .map(columns -> new FileEntry(
                                    columns[1],
                                    "F".equals(columns[0]),
                                    Double.parseDouble(columns[2])
                            ))
                            .forEach(fileEntries::add);
                } catch (IOException e) {
                    LOGGER.error("Error when importing names from SSA file.", e);
                }
            }
            LOGGER.info("{} entries in raw data.", fileEntries.size());

            final var gcCounter = new MutableInt(0);

            fileEntries.stream()
                    .collect(Collectors.groupingBy(fileEntry -> fileEntry.name))
                    .forEach((s, entries) -> {
                        var malePercent = entries.stream().filter(fileEntry -> !fileEntry.isFemale).mapToDouble(fileEntry -> fileEntry.percentage).average().orElse(0.0);
                        var femalePercent = entries.stream().filter(fileEntry -> fileEntry.isFemale).mapToDouble(fileEntry -> fileEntry.percentage).average().orElse(0.0);

                        var firstName = entries.get(0);

                        final var expectedPercentOfPopulation = 0.01 * (malePercent + femalePercent);
                        final var expectedPercentWomen = expectedPercentOfPopulation > 0 ? 0.01 * femalePercent / expectedPercentOfPopulation : null;

                        addName(firstName.name, expectedPercentOfPopulation, expectedPercentWomen, Country.USA);

                        gcCounter.increment();
                        if (gcCounter.intValue() % 1000 == 0) {
                            LOGGER.info("{} names processed. Time for garbage collection.", gcCounter.intValue());
                            System.gc();
                        }
                    });

            System.gc();
            LOGGER.info("SSA data sync done.");
        }, Instant.now().plusSeconds(1));
    }
}
