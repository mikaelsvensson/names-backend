package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.AttributeKey;
import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.NoSuchElementException;

@Component
public class SyllableUpdater {

    private static final int BOOT_ORDER = ScbNameImporter.BOOT_ORDER + 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(SyllableUpdater.class);

    private static final String SYSTEM_NAME = "syllableUpdater";

    @Value("${syllableUpdater.onStart:true}")
    private boolean onStart;

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
            LOGGER.info("Syllable update skipped.");
            return;
        }
        scheduler.schedule(() -> {
            final var gcCounter = new MutableInt(0);

            LOGGER.info("Syllable update started");
            try {
                final var names = namesRepository.all(null, null, 0, Integer.MAX_VALUE, null, null, null);
                while (names.hasNext()) {
                    Name name = names.next();
                    final var expectedSyllableCount = Double.valueOf(NameFeature.syllableCount(name.getName()));
                    final var attribute = name.getAttribute(AttributeKey.SYLLABLE_COUNT);
                    if (attribute.isEmpty() || !expectedSyllableCount.equals(attribute.get().getValue())) {
                        namesRepository.setNumericAttribute(name, getUser(), AttributeKey.SYLLABLE_COUNT, expectedSyllableCount);
                    }

                    gcCounter.increment();
                    if (gcCounter.intValue() % 1000 == 0) {
                        LOGGER.info("{} names processed. Time for garbage collection.", gcCounter.intValue());
                        System.gc();
                    }
                }
                LOGGER.info("Syllable update done");
                System.gc();
            } catch (NameException e) {
                LOGGER.error("Error when checking syllable counts.", e);
            } catch (NoSuchElementException e) {
                LOGGER.error("No names in database.", e);
            }
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
