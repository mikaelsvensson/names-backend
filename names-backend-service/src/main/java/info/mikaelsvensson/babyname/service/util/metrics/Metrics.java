package info.mikaelsvensson.babyname.service.util.metrics;

import org.apache.commons.lang3.mutable.MutableInt;
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
}
