package info.mikaelsvensson.babyname.service.repository.users;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.repository.names.NameAddedEvent;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
@Service
public class DbUserRepository implements UserRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final ApplicationEventPublisher applicationEventPublisher;

    public DbUserRepository(
            @Autowired NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Autowired ApplicationEventPublisher applicationEventPublisher) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

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
            final var user = new User(IdUtils.random(), Instant.now());
            namedParameterJdbcTemplate.update(
                    "INSERT INTO users (id, system_name, created_at) VALUES (:id, NULL, :createdAt)",
                    Map.of(
                            "id", user.getId(),
                            "createdAt", user.getCreatedAt().toEpochMilli()
                    ));
            applicationEventPublisher.publishEvent(new UserAddedEvent(user, this));
            return user;
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User addFromProvider(UserProvider provider, String providerValue) throws UserException {
        try {
            final var user = new User(IdUtils.random(), provider, providerValue, null, Instant.now());
            namedParameterJdbcTemplate.update(
                    "INSERT INTO users (id, system_name, created_at) VALUES (:id, :systemName, :createdAt)",
                    Map.of(
                            "id", user.getId(),
                            "systemName",
                            provider == UserProvider.INTERNAL
                                    ? user.getProviderUserId()
                                    : provider.name().toLowerCase() + ":" + providerValue,
                            "createdAt", user.getCreatedAt().toEpochMilli()
                    ));
            applicationEventPublisher.publishEvent(new UserAddedEvent(user, this));
            return user;
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User get(String userId) throws UserException {
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT u.*, r.related_user_id FROM users AS u LEFT JOIN relationships AS r ON u.id = r.user_id WHERE u.id = :id",
                    Map.of(
                            "id", userId
                    ),
                    DbUserRepository::mapper);
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User getByProvider(UserProvider provider, String providerValue) throws UserException {
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT u.*, r.related_user_id FROM users AS u LEFT JOIN relationships AS r ON u.id = r.user_id WHERE u.system_name = :systemName",
                    Map.of(
                            "systemName",
                            provider == UserProvider.INTERNAL
                                    ? providerValue
                                    : provider.name().toLowerCase() + ":" + providerValue
                    ),
                    DbUserRepository::mapper);
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public void delete(User user) throws UserException {
        try {
            namedParameterJdbcTemplate.update(
                    "DELETE FROM users WHERE id = :id",
                    Map.of(
                            "id", user.getId()
                    ));
            applicationEventPublisher.publishEvent(new UserDeletedEvent(user, this));
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    public static User mapper(ResultSet resultSet, int index) throws SQLException {
        final var providerData = Optional.ofNullable(resultSet.getString("system_name")).orElse(UserProvider.ANONYMOUS.name().toLowerCase() + ":");
        final var matcher = Pattern.compile("^((?<key>[a-z]+):)?(?<value>.*)$").matcher(providerData);
        matcher.find();
        final var key = Optional
                .ofNullable(matcher.group("key"))
                .map(String::toUpperCase)
                .map(UserProvider::valueOf)
                .orElse(UserProvider.INTERNAL);
        final var value = matcher.group("value");
        return new User(
                resultSet.getString("id"),
                key,
                value,
                // About storing dates as instants: https://engineering.q42.nl/why-always-use-utc-is-bad-advice/
                resultSet.getString("related_user_id"),
                Instant.ofEpochMilli(resultSet.getLong("created_at")));
    }
}
