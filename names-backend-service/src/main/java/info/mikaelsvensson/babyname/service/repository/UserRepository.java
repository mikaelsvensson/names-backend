package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository {
    List<User> all() throws UserException;

    User add() throws UserException;

    User addSystemUser(String systemName) throws UserException;

    User get(String userId) throws UserException;

    User getBySystemName(String systemName) throws UserException;
}
