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
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SsbNameImporter extends AbstractNameImporter {

    public static final int BOOT_ORDER = 20;
    private static final int COUNT = 250;

    public SsbNameImporter(
            @Autowired NamesRepository namesRepository,
            @Autowired UserRepository userRepository,
            @Value("${ssbImporter.onStart:true}") boolean onStart,
            @Value("classpath:names/no/boys.csv") Resource databaseBoys,
            @Value("classpath:names/no/girls.csv") Resource databaseGirls,
            @Autowired TaskScheduler scheduler) {
        super(SYSTEM_NAME, namesRepository, userRepository);
        this.onStart = onStart;
        this.databaseBoys = databaseBoys;
        this.databaseGirls = databaseGirls;
        this.scheduler = scheduler;
    }

    private static class NamePopularity {
        private double percentMen = 0.0;
        private double percentWomen = 0.0;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SsbNameImporter.class);

    public static final String SYSTEM_NAME = "ssbImporter";

    private final boolean onStart;

    private final Resource databaseBoys;
    private final Resource databaseGirls;
    private final TaskScheduler scheduler;

    // About event listener: https://www.baeldung.com/running-setup-logic-on-startup-in-spring
    @EventListener
    @Order(BOOT_ORDER)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!onStart) {
            LOGGER.info("SSB data sync skipped.");
            return;
        }
        scheduler.schedule(this::load, Instant.now().plusSeconds(1));
    }

    void load() {
        LOGGER.info("SSB data sync started.");
        final var fileEntries = new HashMap<String, NamePopularity>();
        for (Resource database : new Resource[]{databaseBoys, databaseGirls}) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(database.getInputStream(), StandardCharsets.ISO_8859_1))) {
                reader.lines()
                        .map(line -> Pattern.compile(";").split(line))
                        .filter(columns -> columns.length > 1)
                        .filter(columns -> "\"Andel av f\u00F8dte (prosent)\"".equals(columns[0]))
                        .forEach(columns -> {
                            final var name = columns[1].substring(1, columns[1].length() - 1);
                            fileEntries.putIfAbsent(name, new NamePopularity());
                            final var percent = Stream.of(columns).skip(9).map(s -> ".".equals(s) ? "0" : s).mapToDouble(Double::valueOf).average().orElse(0.0) / 100.0;
                            if (databaseGirls == database) {
                                fileEntries.get(name).percentWomen = percent;
                            } else {
                                fileEntries.get(name).percentMen = percent;
                            }
                        });
            } catch (IOException e) {
                LOGGER.error("Error when importing names from SSB file.", e);
            }
        }

        LOGGER.info("{} entries in raw data.", fileEntries.size());

        final var gcCounter = new MutableInt(0);

        fileEntries.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, NamePopularity>>comparingDouble(o -> o.getValue().percentWomen + o.getValue().percentMen).reversed())
                .limit(COUNT)
                .forEach(entry -> {
                    final var malePercent = entry.getValue().percentMen;
                    final var femalePercent = entry.getValue().percentWomen;

                    final var name = entry.getKey();
                    final var totalPercent = femalePercent + malePercent;

                    final var expectedPercentOfPopulation = totalPercent;
                    final var expectedPercentWomen = totalPercent > 0 ? 1.0 * femalePercent / totalPercent : null;

                    addName(name, expectedPercentOfPopulation, expectedPercentWomen, Country.NORWAY);

                    gcCounter.increment();
                    if (gcCounter.intValue() % 1000 == 0) {
                        LOGGER.info("{} names processed. Time for garbage collection.", gcCounter.intValue());
                        System.gc();
                    }
                });

        System.gc();
        LOGGER.info("SSB data sync done.");
    }
}
