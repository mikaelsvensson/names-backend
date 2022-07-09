package info.mikaelsvensson.babyname.service.model;

import java.util.Map;

public class Action {
    private String id;
    private ActionType type;
    private String createdBy;
    private ActionStatus status;
    private Map<String, String> parameters;

    public Action() {
    }

    public Action(String id, ActionType type, String createdBy, ActionStatus status, Map<String, String> parameters) {
        this.id = id;
        this.type = type;
        this.createdBy = createdBy;
        this.status = status;
        this.parameters = parameters;
    }

    public ActionType getType() {
        return type;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getId() {
        return id;
    }
}
