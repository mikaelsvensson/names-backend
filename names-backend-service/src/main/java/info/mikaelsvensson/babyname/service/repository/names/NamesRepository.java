package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.Attribute;
import info.mikaelsvensson.babyname.service.model.AttributeKey;
import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface NamesRepository {
    List<Name> all(Set<String> userIds, String namePrefix, int offset, int limit, Set<String> voteUserIds, Set<FilterAttributeNumeric> numericFilters, Set<FilterVote> filterVotes) throws NameException;

    Name add(String name, User user, Set<Attribute<?>> attributes) throws NameException;

    Name get(String nameId) throws NameException;

    void setNumericAttribute(Name name, User attributeOwner, AttributeKey key, Double value) throws NameException;
}
