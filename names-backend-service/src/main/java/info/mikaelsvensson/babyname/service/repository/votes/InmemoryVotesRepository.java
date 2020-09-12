package info.mikaelsvensson.babyname.service.repository.votes;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.model.VoteType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("in-memory")
public class InmemoryVotesRepository implements VotesRepository {

    private final Map<String, Map<String, Vote>> votes = new HashMap<>();

    @Override
    public List<Vote> all(User user) {
        return new ArrayList<>(votes.getOrDefault(user.getId(), new HashMap<>()).values());
    }

    @Override
    public void set(User user, Name name, VoteType voteType) {
        votes.putIfAbsent(user.getId(), new HashMap<>());
        final var nameVotes = votes.get(user.getId());
        nameVotes.put(name.getId(), new Vote(name.getId(), voteType));
    }
}
