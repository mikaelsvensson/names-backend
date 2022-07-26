package info.mikaelsvensson.babyname.service.repository.users;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.common.collect.ImmutableMap;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.repository.Datastore;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
@Service
public class FirestoreUserRepository implements UserRepository {

    private Firestore db;

    public FirestoreUserRepository() {
        db = Datastore.get();
    }

    @Override
    public List<User> all() throws UserException {
        try {
            var result = new ArrayList<User>();
            for (var documentReference : db.collection("users").listDocuments()) {
                result.add(toUser(documentReference.get().get()));
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User add() throws UserException {
        return addFromProvider(UserProvider.ANONYMOUS, null);
    }

    @Override
    public User addFromProvider(UserProvider provider, String providerValue) throws UserException {
        var user = new User(IdUtils.random(), provider, providerValue, null, Instant.now());
        try {
            getByProvider(provider, providerValue);
            throw new UserException("User already exists.");
        } catch (UserException ignored) {
        }
        DocumentReference dbUserRef = db.collection("users").document(user.getId());
        HashMap<String, Object> userProps = new HashMap<>();
        userProps.put("providerKey", user.getProviderKey());
        userProps.put("providerUserId", user.getProviderUserId());
        userProps.put("relatedUserId", user.getRelatedUserId());
        userProps.put("createdAt", user.getCreatedAt().toEpochMilli());
        dbUserRef.set(userProps);
        return user;
    }

    @Override
    public User get(String userId) throws UserException {
        try {
            DocumentReference dbUserRef = db.collection("users").document(userId);
            DocumentSnapshot dbUserSnapshot = dbUserRef.get().get();
            if (!dbUserSnapshot.exists()) {
                throw new UserException("Could not find user " + userId);
            }
            return toUser(dbUserSnapshot);
        } catch (InterruptedException | ExecutionException e) {
            throw new UserException(e.getMessage());
        }
    }

    private static User toUser(DocumentSnapshot dbUser) {
        return new User(
                dbUser.getId(),
                UserProvider.valueOf(dbUser.getString("providerKey")),
                dbUser.getString("providerUserId"),
                dbUser.getString("relatedUserId"),
                Instant.ofEpochMilli(dbUser.getLong("createdAt"))
        );
    }

    @Override
    public User getByProvider(UserProvider provider, String providerValue) throws UserException {
        try {
            QuerySnapshot existingUserResult = db.collection("users")
                    .whereEqualTo("providerKey", provider.name())
                    .whereEqualTo("providerUserId", providerValue)
                    .limit(1)
                    .get().get();
            if (!existingUserResult.isEmpty()) {
                return toUser(existingUserResult.getDocuments().iterator().next());
            } else {
                throw new UserException("User missing.");
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public void delete(User user) throws UserException {
        try {
            db.collection("users").document(user.getId()).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new UserException(e.getMessage());
        }
    }
}
