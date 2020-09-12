package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.Name;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface NamesRepository {
    List<Name> all(Set<String> userIds, String namePrefix, int limit, CountRange countRange, Set<String> voteUserIds) throws NameException;

    Name add(String name, Boolean isMale, Boolean isFemale, boolean isPublic, String ownerUserId) throws NameException;

    Name get(String nameId) throws NameException;
}
