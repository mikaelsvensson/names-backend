package info.mikaelsvensson.babyname.service.repository.votes;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.common.collect.ImmutableMap;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.repository.Datastore;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.names.request.FilterVote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static info.mikaelsvensson.babyname.service.repository.names.request.FilterVoteCondition.*;

@Repository
@Service
public class FirestoreVotesRepository implements VotesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirestoreVotesRepository.class);
    private final Firestore db;

    public FirestoreVotesRepository() {
        db = Datastore.get();
    }

    @Override
    public List<Vote> all(User user) throws VoteException {
        try {
            var votes = new ArrayList<Vote>();
            for (var documentReference : db
                    .collection("votes")
                    .whereEqualTo("user_id", user.getId())
                    .get()
                    .get()
                    .getDocuments()) {
                votes.add(toVote(documentReference.getReference()));
            }
            return votes;
        } catch (InterruptedException | ExecutionException e) {
            throw new VoteException(e.getMessage());
        }
    }

    private static Vote toVote(DocumentReference dbVoteRef) throws VoteException {
        try {
            DocumentSnapshot dbVote = dbVoteRef.get().get();
            return new Vote(
                    dbVote.getString("user_id"),
                    dbVote.getString("name_id"),
                    dbVote.getLong("value"));
        } catch (InterruptedException | ExecutionException e) {
            throw new VoteException(e.getMessage());
        }
    }

    private static String key(User user, Name name) {
        return String.format("u-%s--n-%s", user.getId(), name.getId());
    }

    @Override
    public void set(User user, Name name, Long value) throws VoteException {
        db
                .collection("votes")
                .document(key(user, name))
                .set(
                        ImmutableMap.of(
                                "value", value,
                                "user_id", user.getId(),
                                "name_id", name.getId()
                        )
                );
    }

    public List<Vote> all(FilterVote filter) throws NameException {
        if (filter.getUserIds().isEmpty()) {
            throw new NameException("List of user ids cannot be empty.");
        }
        var query = db.collection("votes").limit(10_000);
        query = switch (filter.getCondition()) {
            case NOT_YET_VOTED -> throw new NameException("NOT_YET_VOTED is not supported.");
            case ANY_VOTE -> query;
            case POSITIVE_VOTE -> query.whereGreaterThan("value", 0);
            case NOT_NEUTRAL_VOTE -> query.whereNotEqualTo("value", 0);
        };
        if (!filter.getUserIds().isEmpty()) {
            query = query.whereIn("user_id", Arrays.asList(filter.getUserIds().toArray()));
        }
        var result = new ArrayList<Vote>();
        try {
            for (QueryDocumentSnapshot document : query.get().get().getDocuments()) {
                result.add(toVote(document.getReference()));
            }
        } catch (InterruptedException | ExecutionException | VoteException e) {
            throw new NameException(e.getMessage());
        }
        return result;
    }
}
