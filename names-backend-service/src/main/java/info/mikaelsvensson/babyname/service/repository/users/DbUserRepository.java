package info.mikaelsvensson.babyname.service.repository.users;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import info.mikaelsvensson.babyname.service.util.metrics.MetricEvent;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final Metrics metrics;

    public DbUserRepository(
            @Autowired NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Autowired Metrics metrics) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.metrics = metrics;
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
            metrics.logEvent(MetricEvent.USER_ADDED);
            return user;
        } catch (DataAccessException e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public User addFromProvider(UserProvider provider, String providerValue) throws UserException {
        try {
            final var user = new User(IdUtils.random(), provider, providerValue, Instant.now());
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
            metrics.logEvent(MetricEvent.USER_ADDED);
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
    public User getByProvider(UserProvider provider, String providerValue) throws UserException {
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE system_name = :systemName",
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
                Instant.ofEpochMilli(resultSet.getLong("created_at")));
    }
}
