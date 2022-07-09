package info.mikaelsvensson.babyname.service.repository.relationships;

import info.mikaelsvensson.babyname.service.model.User;

import java.util.List;

public interface RelationshipsRepository {
    void connect(User user, User relatedUser) throws RelationshipException;
}
