package info.mikaelsvensson.babyname.service.repository;

import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("db-firestore")
public class FirestoreRepositoryHealthcheck implements RepositoryHealthcheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirestoreRepositoryHealthcheck.class);

    private final Firestore db;

    public FirestoreRepositoryHealthcheck() {
        db = Datastore.get();
    }

    @Override
    public void ping() throws Exception {
        db.listCollections().forEach(collectionReference -> {
            LOGGER.info(String.format("Ping found collection %s", collectionReference.getId()));
        });
    }
}
