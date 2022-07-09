package info.mikaelsvensson.babyname.service.repository.users;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.name.Name;
import org.springframework.context.ApplicationEvent;

public class UserAddedEvent extends ApplicationEvent {
    private final User user;

    public UserAddedEvent(User user, Object eventSource) {
        super(eventSource);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
