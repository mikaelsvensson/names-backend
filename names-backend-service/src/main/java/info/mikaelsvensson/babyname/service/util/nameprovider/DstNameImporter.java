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
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class DstNameImporter extends AbstractNameImporter {

    public static final int BOOT_ORDER = 20;
    private static final int COUNT = 250;

    private final Pattern patternGirlsSection = Pattern.compile("<caption>Girl's names</caption>.*?<tbody>(.*?)</tbody>", Pattern.DOTALL);
    private final Pattern patternBoysSection = Pattern.compile("<caption>Boy's names</caption>.*?<tbody>(.*?)</tbody>", Pattern.DOTALL);
    private final Pattern nameObjectWrapperPattern = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL);
    private final Pattern nameObjectPropsPattern = Pattern.compile("<td>(.*?)</td>.*?<td>(.*?)</td>.*?<td>(.*?)</td>.*?<td>(.*?)</td>", Pattern.DOTALL);

    public DstNameImporter(
            @Autowired NamesRepository namesRepository,
            @Autowired UserRepository userRepository,
            @Value("${dstImporter.onStart:true}") boolean onStart,
            @Value("classpath:names/dk/*.html") Resource[] databases,
            @Autowired TaskScheduler scheduler) {
        super(SYSTEM_NAME, namesRepository, userRepository);
        this.onStart = onStart;
        this.databases = databases;
        this.scheduler = scheduler;
    }

    private static class NamePopularity {
        private double perThousandMenAccumulated = 0.0;
        private double perThousandWomenAccumulated = 0.0;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DstNameImporter.class);

    public static final String SYSTEM_NAME = "dstImporter";

    private final boolean onStart;

    private final Resource[] databases;

    private final TaskScheduler scheduler;

    // About event listener: https://www.baeldung.com/running-setup-logic-on-startup-in-spring
    @EventListener
    @Order(BOOT_ORDER)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!onStart) {
            LOGGER.info("DST data sync skipped.");
            return;
        }
        scheduler.schedule(this::load, Instant.now().plusSeconds(1));
    }

    void load() {
        LOGGER.info("DST data sync started.");
        final var fileEntries = new HashMap<String, NamePopularity>();
        for (Resource database : databases) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(database.getInputStream(), StandardCharsets.UTF_8))) {
                final var raw = FileCopyUtils.copyToString(reader);
                for (Pattern sectionPattern : new Pattern[]{patternGirlsSection, patternBoysSection}) {
                    final var sectionMatcher = sectionPattern.matcher(raw);
                    if (sectionMatcher.find()) {
                        final var sectionText = sectionMatcher.group(1);
                        final var matcher = nameObjectWrapperPattern.matcher(sectionText);
                        while (matcher.find()) {
                            final var namePropMatcher = nameObjectPropsPattern.matcher(matcher.group(1));
                            if (namePropMatcher.find()) {
                                final var name = namePropMatcher.group(2).trim();
                                fileEntries.putIfAbsent(name, new NamePopularity());
                                final var perThousand = Double.parseDouble(namePropMatcher.group(4));
                                if (sectionPattern == patternGirlsSection) {
                                    fileEntries.get(name).perThousandWomenAccumulated += perThousand;
                                } else {
                                    fileEntries.get(name).perThousandMenAccumulated += perThousand;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error when importing names from DST file.", e);
            }
        }

        LOGGER.info("{} entries in raw data.", fileEntries.size());

        final var gcCounter = new MutableInt(0);

        fileEntries.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, NamePopularity>>comparingDouble(o -> (o.getValue().perThousandWomenAccumulated + o.getValue().perThousandMenAccumulated) / databases.length).reversed())
                .limit(COUNT)
                .forEach(entry -> {
                    final var malePercent = entry.getValue().perThousandMenAccumulated / 1000/ databases.length;
                    final var femalePercent = entry.getValue().perThousandWomenAccumulated / 1000 / databases.length;

                    final var name = entry.getKey();
                    final var totalPercent = femalePercent + malePercent;

                    final var expectedPercentOfPopulation = totalPercent;
                    final var expectedPercentWomen = totalPercent > 0 ? 1.0 * femalePercent / totalPercent : null;

                    addName(name, expectedPercentOfPopulation, expectedPercentWomen, Country.DENMARK);

                    gcCounter.increment();
                    if (gcCounter.intValue() % 1000 == 0) {
                        LOGGER.info("{} names processed. Time for garbage collection.", gcCounter.intValue());
                        System.gc();
                    }
                });

        System.gc();
        LOGGER.info("DST data sync done.");
    }
}
