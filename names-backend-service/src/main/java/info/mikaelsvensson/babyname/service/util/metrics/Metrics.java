package info.mikaelsvensson.babyname.service.util.metrics;

import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.AnonymousUserAddedEvent;
import info.mikaelsvensson.babyname.service.repository.names.NameAddedEvent;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipAddedEvent;
import info.mikaelsvensson.babyname.service.repository.users.UserAddedEvent;
import info.mikaelsvensson.babyname.service.repository.users.UserDeletedEvent;
import info.mikaelsvensson.babyname.service.repository.votes.VoteEvent;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class Metrics {
    private final Map<MetricEvent, MutableInt> eventCounters = new HashMap<>();
    private final Map<MetricEvent, Set<String>> eventStringSets = new HashMap<>();

    public synchronized void logEvent(MetricEvent event) {
        eventCounters.putIfAbsent(event, new MutableInt(0));
        eventCounters.get(event).increment();
    }

    public synchronized Map<MetricEvent, Integer> getAndReset() {
        final var current = eventCounters
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().intValue()));
        eventCounters.clear();
        eventStringSets.clear();
        return current;
    }

    public void logUniqueString(MetricEvent event, String value) {
        eventStringSets.putIfAbsent(event, new HashSet<>());

        final var strings = eventStringSets.get(event);
        strings.add(value);

        eventCounters.putIfAbsent(event, new MutableInt(0));
        eventCounters.get(event).setValue(strings.size());
    }

//    @EventListener
//    public void onNameAddedEvent(NameAddedEvent event) {
//    }

    @EventListener
    public void onRelationshipAddedEvent(RelationshipAddedEvent event) {
        logEvent(MetricEvent.RELATIONSHIP_ADDED);
    }

    @EventListener
    public void onAnonymousUserAddedEvent(AnonymousUserAddedEvent event) {
        logEvent(MetricEvent.ANONYMOUS_USER_CREATED);
    }

    @EventListener
    public void onUserAddedEvent(UserAddedEvent event) {
        logEvent(MetricEvent.USER_ADDED);
    }

    @EventListener
    public void onUserDeletedEvent(UserDeletedEvent event) {
        logEvent(MetricEvent.USER_DELETED);
    }

    @EventListener
    public void onVoteEvent(VoteEvent event) {
        logEvent(MetricEvent.VOTE);
    }
}
