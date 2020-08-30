package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("in-memory")
public class InmemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();

    @Override
    public List<User> all() {
        return users;
    }

    @Override
    public synchronized User add() throws UserException {
        final var user = new User(IdUtils.random(), null, Instant.now());
        if (users.stream().anyMatch(u -> u.getId().equals(user.getId()))) {
            throw new UserException("Duplicate user id.");
        }
        users.add(user);
        return user;
    }

    @Override
    public User addSystemUser(String systemName) throws UserException {
        final var user = new User(IdUtils.random(), systemName, Instant.now());
        if (users.stream().anyMatch(u -> u.getSystemName().equals(user.getSystemName()))) {
            throw new UserException("Duplicate system name.");
        }
        users.add(user);
        return user;
    }

    @Override
    public User get(String userId) throws UserException {
        return users.stream().filter(u -> u.getId().equals(userId)).findFirst().orElseThrow(() -> new UserException("User not found"));
    }

    @Override
    public User getBySystemName(String systemName) throws UserException {
        return users.stream().filter(u -> u.getSystemName().equals(systemName)).findFirst().orElseThrow(() -> new UserException("User not found"));
    }
}
