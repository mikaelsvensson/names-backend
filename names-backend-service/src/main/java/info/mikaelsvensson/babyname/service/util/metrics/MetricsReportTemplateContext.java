package info.mikaelsvensson.babyname.service.util.metrics;

import java.util.ArrayList;
import java.util.List;

public class MetricsReportTemplateContext {
    public static class Event {
        public String name;
        public int count;

        public Event(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    public List<Event> events = new ArrayList<>();

    public MetricsReportTemplateContext() {
    }

    public void addEvent(String name, int count) {
        events.add(new Event(name, count));
    }
}
