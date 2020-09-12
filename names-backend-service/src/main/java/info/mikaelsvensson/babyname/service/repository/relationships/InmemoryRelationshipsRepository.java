package info.mikaelsvensson.babyname.service.repository.relationships;

import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("in-memory")
public class InmemoryRelationshipsRepository implements RelationshipsRepository {
    @Override
    public void add(User user, User relatedUser) throws RelationshipException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<User> getRelatedUsers(User user) throws RelationshipException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
