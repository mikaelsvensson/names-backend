package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.name.MetricsProperties;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.PopulationProperties;
import info.mikaelsvensson.babyname.service.repository.names.request.NamesRequest;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Repository
public interface NamesRepository {
    void all(NamesRequest request, Consumer<Name> nameConsumer) throws NameException;

    Map<String, String> allNames() throws NameException;

    Name add(String name, User user) throws NameException;

    Name add(String name, Map<Country, PopulationProperties> demographics, Set<User> users) throws NameException;

    Name get(String nameId, User user) throws NameException;

    Optional<Name> getByName(String name) throws NameException;

    void setDemographicsProperties(Name name, Country country, PopulationProperties properties) throws NameException;

    void setMetricsProperties(Name name, MetricsProperties properties) throws NameException;
}
