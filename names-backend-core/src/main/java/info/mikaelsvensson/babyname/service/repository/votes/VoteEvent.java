package info.mikaelsvensson.babyname.service.repository.votes;

import org.springframework.context.ApplicationEvent;

public class VoteEvent extends ApplicationEvent {
    public VoteEvent(Object eventSource) {
        super(eventSource);
    }
}
