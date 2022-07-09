package info.mikaelsvensson.babyname.service.repository.users;

import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.context.ApplicationEvent;

public class UserDeletedEvent extends ApplicationEvent {
    private final User user;

    public UserDeletedEvent(User user, Object eventSource) {
        super(eventSource);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
