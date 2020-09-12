package info.mikaelsvensson.babyname.service.repository.actions;

import info.mikaelsvensson.babyname.service.model.Action;
import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Profile("in-memory")
public class InmemoryActionsRepository implements ActionsRepository {
    @Override
    public Action get(String id) throws ActionException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Action add(User creator, ActionType actionType, Map<String, String> actionParameters, ActionStatus status) throws ActionException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setStatus(Action action, ActionStatus newStatus) throws ActionException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
