package info.mikaelsvensson.babyname.service.repository.relationships;

import com.google.cloud.firestore.Firestore;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Repository
@Service
@Profile("db-firestore")
public class FirestoreRelationshipsRepository implements RelationshipsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirestoreRelationshipsRepository.class);

    private final Firestore db;

    public FirestoreRelationshipsRepository() {
        db = Datastore.get();
    }

    @Override
    public void connect(User user, User relatedUser) throws RelationshipException {
        db.collection("users").document(user.getId()).update("relatedUserId", relatedUser.getId());
        db.collection("users").document(relatedUser.getId()).update("relatedUserId", user.getId());
    }
}
