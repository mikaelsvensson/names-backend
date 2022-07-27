package info.mikaelsvensson.babyname.service.util.nameprovider;

import info.mikaelsvensson.babyname.service.repository.names.Country;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class AvoindataNameImporter extends AbstractNameImporter {

    private static final int COUNT = 1000;

    public AvoindataNameImporter(
            @Autowired NamesRepository namesRepository,
            @Autowired UserRepository userRepository,
            @Value("classpath:names/fi/men-20210205.csv") Resource databaseBoys,
            @Value("classpath:names/fi/women-20210205.csv") Resource databaseGirls) {
        super(SYSTEM_NAME, namesRepository, userRepository);
        this.databaseBoys = databaseBoys;
        this.databaseGirls = databaseGirls;
    }

    private static class NamePopularity {
        private int countMen = 0;
        private int countWomen = 0;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AvoindataNameImporter.class);

    public static final String SYSTEM_NAME = "avoindataImporter";

    private final Resource databaseBoys;
    private final Resource databaseGirls;

    public void load() {
        LOGGER.info("Avoindata data sync started.");
        final var fileEntries = new HashMap<String, NamePopularity>();
        for (Resource database : new Resource[]{databaseBoys, databaseGirls}) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(database.getInputStream(), StandardCharsets.UTF_8))) {
                reader.lines()
                        .map(line -> Pattern.compile(";").split(line))
                        .filter(columns -> columns.length == 2)
                        .forEach(columns -> {
                            final var name = columns[0];
                            fileEntries.putIfAbsent(name, new NamePopularity());
                            final var percent = Integer.parseInt(columns[1]);
                            if (databaseGirls == database) {
                                fileEntries.get(name).countWomen = percent;
                            } else {
                                fileEntries.get(name).countMen = percent;
                            }
                        });
            } catch (IOException e) {
                LOGGER.error("Error when importing names from Avoindata file.", e);
            }
        }

        LOGGER.info("{} entries in raw data.", fileEntries.size());

        final var totalPeopleCount = fileEntries.values().stream().mapToLong(fileEntry -> fileEntry.countWomen + fileEntry.countMen).sum();

        final var gcCounter = new MutableInt(0);

        fileEntries.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, NamePopularity>>comparingDouble(o -> o.getValue().countWomen + o.getValue().countMen).reversed())
                .limit(COUNT)
                .forEach(entry -> {
                    final var malePercent = 1.0 * entry.getValue().countMen / totalPeopleCount;
                    final var femalePercent = 1.0 * entry.getValue().countWomen / totalPeopleCount;

                    final var name = entry.getKey();
                    final var totalPercent = femalePercent + malePercent;

                    final var expectedPercentOfPopulation = totalPercent;
                    final var expectedPercentWomen = totalPercent > 0 ? 1.0 * femalePercent / totalPercent : null;

                    addName(name, expectedPercentOfPopulation, expectedPercentWomen, Country.FINLAND);

                    gcCounter.increment();
                    if (gcCounter.intValue() % 1000 == 0) {
                        LOGGER.info("{} names processed. Time for garbage collection.", gcCounter.intValue());
                        System.gc();
                    }
                });

        System.gc();
        LOGGER.info("Avoindata data sync done.");
    }
}
