package info.mikaelsvensson.babyname.service.repository.users;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
@Service
@Profile("db")
public class DbUserRepository implements UserRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<User> all() throws UserException {
        try {
            return namedParameterJdbcTemplate.query(
                    "SELECT * FROM users",
                    DbUserRepository::mapper);
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User add() throws UserException {
        try {
            final var user = new User(IdUtils.random(), null, Instant.now());
            namedParameterJdbcTemplate.update(
                    "INSERT INTO users (id, system_name, created_at) VALUES (:id, NULL, :createdAt)",
                    Map.of(
                            "id", user.getId(),
                            "createdAt", user.getCreatedAt().toEpochMilli()
                    ));
            return user;
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User addSystemUser(String systemName) throws UserException {
        try {
            final var user = new User(IdUtils.random(), systemName, Instant.now());
            namedParameterJdbcTemplate.update(
                    "INSERT INTO users (id, system_name, created_at) VALUES (:id, :systemName, :createdAt)",
                    Map.of(
                            "id", user.getId(),
                            "systemName", user.getSystemName(),
                            "createdAt", user.getCreatedAt().toEpochMilli()
                    ));
            return user;
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User get(String userId) throws UserException {
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE id = :id",
                    Map.of(
                            "id", userId
                    ),
                    DbUserRepository::mapper);
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User getBySystemName(String systemName) throws UserException {
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE system_name = :systemName",
                    Map.of(
                            "systemName", systemName
                    ),
                    DbUserRepository::mapper);
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    public static User mapper(ResultSet resultSet, int index) throws SQLException {
        return new User(
                resultSet.getString("id"),
                resultSet.getString("system_name"),
                // About storing dates as instants: https://engineering.q42.nl/why-always-use-utc-is-bad-advice/
                Instant.ofEpochMilli(resultSet.getLong("created_at")));
    }
}
