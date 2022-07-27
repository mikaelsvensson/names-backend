package info.mikaelsvensson.babyname.service.repository.actions;

import com.google.cloud.firestore.Firestore;
import info.mikaelsvensson.babyname.service.model.Action;
import info.mikaelsvensson.babyname.service.model.ActionStatus;
import info.mikaelsvensson.babyname.service.model.ActionType;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.Datastore;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
@Service
@Profile("db-firestore")
public class FirestoreActionsRepository implements ActionsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirestoreActionsRepository.class);
    private final Firestore db;

    public FirestoreActionsRepository() {
        this.db = Datastore.get();
    }

    @Override
    public Action get(String id) throws ActionException {
        try {
            return db.collection("actions").document(id).get().get().toObject(Action.class);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(String.format("Could not get action %s", id), e);
            throw new ActionException(e.getMessage());
        }
    }

    @Override
    public Action add(User creator, ActionType actionType, Map<String, String> actionParameters, ActionStatus status) throws ActionException {
        Action action = new Action(IdUtils.random(), actionType, creator.getId(), status, actionParameters);
        db.collection("actions").document(action.getId()).set(action);
        return action;
    }

    @Override
    public void setStatus(Action action, ActionStatus newStatus) throws ActionException {
        db.collection("actions").document(action.getId()).update("status", newStatus);
    }
}
