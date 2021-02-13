package info.mikaelsvensson.babyname.service.repository.votes;

import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VotesRepository {
    List<Vote> all(User user) throws VoteException;

    void set(User user, Name name, Long value) throws VoteException;
}
