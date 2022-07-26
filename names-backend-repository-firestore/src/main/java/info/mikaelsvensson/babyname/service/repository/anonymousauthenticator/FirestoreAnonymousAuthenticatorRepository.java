package info.mikaelsvensson.babyname.service.repository.anonymousauthenticator;

import com.google.cloud.firestore.Firestore;
import info.mikaelsvensson.babyname.service.repository.Datastore;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

@Repository
@Service
public class FirestoreAnonymousAuthenticatorRepository implements AnonymousAuthenticatorRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirestoreAnonymousAuthenticatorRepository.class);

    private final Firestore db;

    public FirestoreAnonymousAuthenticatorRepository() {
        this.db = Datastore.get();
    }

    @Override
    public boolean exists(String id) throws AnonymousAuthenticatorException {
        try {
            return db.collection("anonymous_users").document(id).get().get().exists();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(String.format("Could not check if anonymous user %s exists.", id), e);
            return false;
        }
    }

    @Override
    public void logUse(String id) throws AnonymousAuthenticatorException {
        db.collection("anonymous_users")
                .document(id)
                .set(Collections.singletonMap("used_at", Instant.now().toEpochMilli()));
    }

    @Override
    public String create() throws AnonymousAuthenticatorException {
        var id = IdUtils.random();
        logUse(id);
        return id;
    }

    @Override
    public long count() throws AnonymousAuthenticatorException {
        LOGGER.warn("FirestoreAnonymousAuthenticatorRepository.count() always returns 0.");
        return 0;
    }
}
