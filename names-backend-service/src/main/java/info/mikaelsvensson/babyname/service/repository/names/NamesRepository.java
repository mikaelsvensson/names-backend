package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.Attribute;
import info.mikaelsvensson.babyname.service.model.AttributeKey;
import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Repository
public interface NamesRepository {
    void all(Set<String> userIds, String namePrefix, int offset, int limit, Set<String> voteUserIds, Set<FilterAttributeNumeric> numericFilters, Set<FilterVote> filterVotes, Consumer<Name> nameConsumer) throws NameException;

    Name add(String name, User user, Set<Attribute<?>> attributes) throws NameException;

    Name get(String nameId) throws NameException;

    Optional<Name> getByName(String name) throws NameException;

    void setNumericAttribute(Name name, User attributeOwner, AttributeKey key, Double value) throws NameException;
}
