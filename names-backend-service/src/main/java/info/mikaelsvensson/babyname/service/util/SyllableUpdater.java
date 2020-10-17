package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.model.AttributeKey;
import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.NamesRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class SyllableUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyllableUpdater.class);

    public static final String SYSTEM_NAME = "syllableUpdater";

    @Autowired
    private NamesRepository namesRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    // About event listener: https://www.baeldung.com/running-setup-logic-on-startup-in-spring
    @EventListener
    @Order(1000)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            for (Name name : namesRepository.all(null, null, 0, Integer.MAX_VALUE, null, null)) {

                final var expectedSyllableCount = Double.valueOf(NameFeature.syllableCount(name.getName()));
                final var attribute = name.getAttribute(AttributeKey.SYLLABLE_COUNT);
                if (attribute.isEmpty() || !expectedSyllableCount.equals(attribute.get().getValue())) {
                    namesRepository.setNumericAttribute(name, getUser(), AttributeKey.SYLLABLE_COUNT, expectedSyllableCount);
                }
            }
        } catch (NameException e) {
            LOGGER.error("Error when checking syllable counts.", e);
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
