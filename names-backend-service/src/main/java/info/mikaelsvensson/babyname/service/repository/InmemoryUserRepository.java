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
    public User add() {
        final var user = new User();
        users.add(user);
        return user;
    }
}
