package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.name.Name;
import org.springframework.context.ApplicationEvent;

public class NameAddedEvent extends ApplicationEvent {
    private final Name name;

    public NameAddedEvent(Name name, Object eventSource) {
        super(eventSource);
        this.name = name;
    }

    public Name getName() {
        return name;
    }
}
