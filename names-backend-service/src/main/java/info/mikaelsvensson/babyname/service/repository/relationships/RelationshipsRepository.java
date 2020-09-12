package info.mikaelsvensson.babyname.service.repository.relationships;

import info.mikaelsvensson.babyname.service.model.User;

import java.util.List;

public interface RelationshipsRepository {
    void add(User user, User relatedUser) throws RelationshipException;

    List<User> getRelatedUsers(User user) throws RelationshipException;
}
