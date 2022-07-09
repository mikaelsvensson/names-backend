package info.mikaelsvensson.babyname.service.repository.relationships;

import info.mikaelsvensson.babyname.service.model.name.Name;
import org.springframework.context.ApplicationEvent;

public class RelationshipAddedEvent extends ApplicationEvent {
    public RelationshipAddedEvent(Object eventSource) {
        super(eventSource);
    }
}
