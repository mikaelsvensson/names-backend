package info.mikaelsvensson.babyname.service.repository.anonymousauthenticator;

import org.springframework.context.ApplicationEvent;

public class AnonymousUserAddedEvent extends ApplicationEvent {
    private final String id;

    public AnonymousUserAddedEvent(String id, Object eventSource) {
        super(eventSource);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
