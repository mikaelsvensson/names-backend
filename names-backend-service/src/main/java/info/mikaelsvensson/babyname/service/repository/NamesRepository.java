package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.Name;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NamesRepository {
    List<Name> all();

    Name add(String name, Boolean isMale, Boolean isFemale, String ownerUserId);

    Name get(String nameId) throws NameException;
}
