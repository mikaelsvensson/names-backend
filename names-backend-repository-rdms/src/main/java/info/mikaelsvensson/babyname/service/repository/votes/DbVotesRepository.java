package info.mikaelsvensson.babyname.service.repository.votes;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.model.name.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Repository
@Service
public class DbVotesRepository implements VotesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbVotesRepository.class);

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final ApplicationEventPublisher applicationEventPublisher;

    public DbVotesRepository(
            @Autowired NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Autowired ApplicationEventPublisher applicationEventPublisher) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public List<Vote> all(User user) throws VoteException {
        try {
            return namedParameterJdbcTemplate.query(
                    "SELECT * FROM votes WHERE user_id = :userId",
                    Map.of(
                            "userId", user.getId()
                    ),
                    DbVotesRepository::mapper);
        } catch (DataAccessException e) {
            throw new VoteException(e.getMessage());
        }
    }

    @Override
    public void set(User user, Name name, Long value) throws VoteException {
        final var timestamp = Instant.now().toEpochMilli();
        try {
            final var rowsInserted = namedParameterJdbcTemplate.update("INSERT INTO votes (user_id, name_id, value, created_at, updated_at) VALUES (:userId, :nameId, :value, :createdAt, :updatedAt)",
                    Map.of(
                            "userId", user.getId(),
                            "nameId", name.getId(),
                            "value", value,
                            "createdAt", timestamp,
                            "updatedAt", timestamp
                    ));
            if (rowsInserted != 1) {
                throw new VoteException("Query inserted " + rowsInserted + " rows."); // Throw here, and catch right below.
            }
            LOGGER.info("User {} cast {} vote for name {}.", user.getId(), value, name.getId());
            applicationEventPublisher.publishEvent(new VoteEvent(this));
        } catch (DataAccessException | VoteException e) {
            try {
                final var rowsUpdated = namedParameterJdbcTemplate.update("UPDATE votes SET value = :value, updated_at = :updatedAt WHERE user_id = :userId AND name_id = :nameId",
                        Map.of(
                                "userId", user.getId(),
                                "nameId", name.getId(),
                                "value", value,
                                "updatedAt", timestamp
                        ));
                if (rowsUpdated != 1) {
                    throw new VoteException("Query updated " + rowsUpdated + " rows.");
                }
                LOGGER.info("User {} changed to {} vote for name {}.", user.getId(), value, name.getId());
                applicationEventPublisher.publishEvent(new VoteEvent(this));
            } catch (DataAccessException ex) {
                LOGGER.warn("Failed to cast vote. User {} could not change to {} vote for name {}. Reason: {}.", user.getId(), value, name.getId(), ex.getMessage());
                throw new VoteException(ex.getMessage());
            }
        }
    }

    private static Vote mapper(ResultSet resultSet, int index) throws SQLException {
        return new Vote(
                resultSet.getString("user_id"),
                resultSet.getString("name_id"),
                resultSet.getLong("value"));
    }
}
