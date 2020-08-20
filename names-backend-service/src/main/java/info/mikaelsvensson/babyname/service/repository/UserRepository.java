package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository {
    List<User> all();

    User add();
}
