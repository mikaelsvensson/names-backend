package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.name.MetricsProperties;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.repository.names.NameAddedEvent;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.names.request.BasicNameFacet;
import info.mikaelsvensson.babyname.service.repository.names.request.MetricsNameFacet;
import info.mikaelsvensson.babyname.service.repository.names.request.NamesRequest;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
public class SyllableUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyllableUpdater.class);

    private static final String SYSTEM_NAME = "syllableUpdater";

    @Autowired
    private NamesRepository namesRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    public void load() {
        final var gcCounter = new MutableInt(0);

        LOGGER.info("Syllable update started");
        try {
            namesRepository.all(new NamesRequest().basic(new BasicNameFacet()).metrics(new MetricsNameFacet()), name -> {
                if (name.getMetrics() == null || name.getMetrics().getSyllableCount() == null) {
                    addMetrics(name);
                }

                gcCounter.increment();
                if (gcCounter.intValue() % 1000 == 0) {
                    LOGGER.info("{} names processed. Time for garbage collection.", gcCounter.intValue());
                    System.gc();
                }
            });
            LOGGER.info("Syllable update done");
            System.gc();
        } catch (NameException e) {
            LOGGER.error("Error when checking syllable counts.", e);
        } catch (NoSuchElementException e) {
            LOGGER.error("No names in database.", e);
        }
    }

    @EventListener
    public void onNameAdded(NameAddedEvent event) {
        addMetrics(event.getName());
    }

    private void addMetrics(Name name) {
        try {
            final var syllableCount = NameFeature.syllableCount(name.getName());
            namesRepository.setMetricsProperties(name, new MetricsProperties((double) syllableCount));
        } catch (NameException e) {
            LOGGER.error("Could not update syllable count for name.", e);
        }
    }
}
