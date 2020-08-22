package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InmemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();

    @Override
    public List<User> all() {
        return users;
    }

    @Override
    public synchronized User add() throws UserException {
        final var user = new User();
        if (users.stream().anyMatch(u -> u.getId().equals(user.getId()))) {
            throw new UserException("Duplicate user id.");
        }
        users.add(user);
        return user;
    }

    @Override
    public User get(String userId) throws UserException {
        return users.stream().filter(u -> u.getId().equals(userId)).findFirst().orElseThrow(() -> new UserException("User not found"));
    }
}
