package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("in-memory")
public class InmemoryNamesRepository implements NamesRepository {

    private final List<Name> names = new ArrayList<>();

    private List<Name> getNames() {
        return names;
    }

    @Override
    public List<Name> all(Set<String> userIds, String namePrefix, int limit, CountRange countRange) {
        return getNames().stream()
                .filter(name -> userIds.contains(name.getOwnerUserId()))
                .filter(name -> namePrefix == null || namePrefix.trim().length() == 0 || name.getName().toLowerCase().startsWith(namePrefix.toLowerCase()))
                .filter(name -> countRange == null || countRange.inRange(name))
                .limit(Math.max(0, Math.min(limit, 1000)))
                .collect(Collectors.toList());
    }

    @Override
    public Name add(String name, Boolean isMale, Boolean isFemale, boolean isPublic, String ownerUserId) throws NameException {
        final var newObj = new Name(name, 0, isMale, isFemale, ownerUserId, IdUtils.random(), isPublic);
        getNames().add(newObj);
        return newObj;
    }

    @Override
    public Name get(String nameId) throws NameException {
        return getNames().stream().filter(u -> u.getId().equals(nameId)).findFirst().orElseThrow(() -> new NameException("Name not found"));
    }
}
