package info.mikaelsvensson.babyname.service.repository.actions;

import info.mikaelsvensson.babyname.service.model.Action;
import info.mikaelsvensson.babyname.service.model.ActionStatus;
import info.mikaelsvensson.babyname.service.model.ActionType;
import info.mikaelsvensson.babyname.service.model.User;

import java.util.Map;

public interface ActionsRepository {
    Action get(String id) throws ActionException;

    Action add(User creator, ActionType actionType, Map<String, String> actionParameters, ActionStatus status) throws ActionException;

    void setStatus(Action action, ActionStatus newStatus) throws ActionException;
}
