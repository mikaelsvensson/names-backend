package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.model.VoteType;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class InmemoryVotesRepository implements VotesRepository {

    private final Map<String, Map<String, Vote>> votes = new HashMap<>();

    @Override
    public Collection<Vote> all(User user) {
        return votes.getOrDefault(user.getId(), new HashMap<>()).values();
    }

    @Override
    public void set(User user, Name name, VoteType voteType) {
        votes.putIfAbsent(user.getId(), new HashMap<>());
        final var nameVotes = votes.get(user.getId());
        nameVotes.put(name.getId(), new Vote(name.getId(), voteType));
    }
}
