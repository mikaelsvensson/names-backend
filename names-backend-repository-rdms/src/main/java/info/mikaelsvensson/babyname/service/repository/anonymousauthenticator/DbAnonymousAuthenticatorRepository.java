package info.mikaelsvensson.babyname.service.repository.anonymousauthenticator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Repository
@Service
@Profile("db-rdms")
public class DbAnonymousAuthenticatorRepository implements AnonymousAuthenticatorRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbAnonymousAuthenticatorRepository.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final ApplicationEventPublisher applicationEventPublisher;

    public DbAnonymousAuthenticatorRepository(
            @Autowired NamedParameterJdbcTemplate jdbcTemplate,
            @Autowired ApplicationEventPublisher applicationEventPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public boolean exists(String id) throws AnonymousAuthenticatorException {
        try {
            final var ids = jdbcTemplate.queryForList(
                    "SELECT id FROM anonymousauthenticator_identifiers WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue("id", id),
                    String.class);
            return !ids.isEmpty();
        } catch (DataAccessException e) {
            LOGGER.warn("Could not check database.", e);
            return false;
        }
    }

    @Override
    public void logUse(String id) throws AnonymousAuthenticatorException {
        try {
            final var rowsUpdated = jdbcTemplate.update("UPDATE anonymousauthenticator_identifiers SET used_at = :usedAt WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue("id", id)
                            .addValue("usedAt", Instant.now().toEpochMilli()));
            if (rowsUpdated != 1) {
                LOGGER.warn("Anonymous user exists but used_at value could not be updated.");
                throw new AnonymousAuthenticatorException("Could not log token request");
            }
        } catch (DataAccessException e) {
            LOGGER.warn("Anonymous user exists but used_at value could not be updated.");
            throw new AnonymousAuthenticatorException("Could not log token request");
        }
    }

    @Override
    public String create() throws AnonymousAuthenticatorException {
        try {
            final var id = UUID.randomUUID().toString();
            final var rowsUpdated = jdbcTemplate.update(
                    "INSERT INTO anonymousauthenticator_identifiers (id, created_at, used_at) VALUES (:id, :createdAt, :usedAt)",
                    new MapSqlParameterSource()
                            .addValue("id", id)
                            .addValue("createdAt", Instant.now().toEpochMilli())
                            .addValue("usedAt", Instant.now().toEpochMilli()));
            if (rowsUpdated != 1) {
                LOGGER.warn("Anonymous user could not be created.");
                throw new AnonymousAuthenticatorException("Could not create user");
            }
            applicationEventPublisher.publishEvent(new AnonymousUserAddedEvent(id, this));
            return id;
        } catch (DataAccessException e) {
            LOGGER.warn("Anonymous user could not be created.", e);
            throw new AnonymousAuthenticatorException("Could not create user");
        }
    }

    @Override
    public long count() throws AnonymousAuthenticatorException {
        try {
            final var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM anonymousauthenticator_identifiers", new MapSqlParameterSource(), Long.class);
            if (count != null) {
                return count;
            } else {
                LOGGER.warn("Could not count users (query returned null).");
                throw new AnonymousAuthenticatorException("Could not count users");
            }
        } catch (DataAccessException e) {
            LOGGER.warn("Could not count users.", e);
            throw new AnonymousAuthenticatorException("Could not count users");
        }
    }
}
