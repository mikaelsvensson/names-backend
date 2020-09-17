package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.Attribute;
import info.mikaelsvensson.babyname.service.model.AttributeKey;
import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public interface NamesRepository {
    List<Name> all(Set<String> userIds, String namePrefix, int limit, Set<String> voteUserIds) throws NameException;

    Name add(String name, String ownerUserId, Set<Attribute<?>> attributes) throws NameException;

    Name get(String nameId) throws NameException;

    void setNumericAttributes(Name name, User attributeOwner, Map<AttributeKey, Double> values) throws NameException;
}
