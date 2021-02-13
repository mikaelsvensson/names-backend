package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.model.name.MetricsProperties;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.ScbProperties;
import info.mikaelsvensson.babyname.service.repository.names.request.NamesRequest;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Repository
public interface NamesRepository {
    void all(NamesRequest request, Consumer<Name> nameConsumer) throws NameException;

    Map<String, String> allNames() throws NameException;

    Name add(String name, User user) throws NameException;

    Name get(String nameId, User user) throws NameException;

    Optional<Name> getByName(String name) throws NameException;

    void setScbProperties(Name name, ScbProperties properties) throws NameException;

    void setMetricsProperties(Name name, MetricsProperties properties) throws NameException;
}
