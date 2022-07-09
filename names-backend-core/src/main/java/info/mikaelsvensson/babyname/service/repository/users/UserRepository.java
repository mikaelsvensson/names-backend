package info.mikaelsvensson.babyname.service.repository.users;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository {
    List<User> all() throws UserException;

    User add() throws UserException;

    User addFromProvider(UserProvider provider, String providerValue) throws UserException;

    User get(String userId) throws UserException;

    User getByProvider(UserProvider provider, String providerValue) throws UserException;

    void delete(User user) throws UserException;
}
