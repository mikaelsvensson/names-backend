package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.Vote;
import info.mikaelsvensson.babyname.service.model.VoteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
@Service
@Profile("db")
public class DbVotesRepository implements VotesRepository {

    private static Logger LOGGER = LoggerFactory.getLogger(DbVotesRepository.class);

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<Vote> all(User user) throws VoteException {
        try {
            return namedParameterJdbcTemplate.query(
                    "SELECT * FROM votes",
                    DbVotesRepository::mapper);
        } catch (DataAccessException e) {
            throw new VoteException(e.getMessage());
        }
    }

    @Override
    public void set(User user, Name name, VoteType voteType) throws VoteException {
        try {
            namedParameterJdbcTemplate.update("INSERT INTO votes (user_id, name_id, type) VALUES (:userId, :nameId, :type)",
                    Map.of(
                            "userId", user.getId(),
                            "nameId", name.getId(),
                            "type", voteType.name()
                    ));
            LOGGER.info("User {} cast {} vote for name {}.", user.getId(), voteType.name(), name.getId());
        } catch (DataAccessException e) {
            try {
                namedParameterJdbcTemplate.update("UPDATE votes SET type = :type WHERE user_id = :userId AND name_id = :nameId",
                        Map.of(
                                "userId", user.getId(),
                                "nameId", name.getId(),
                                "type", voteType.name()
                        ));
                LOGGER.info("User {} changed to {} vote for name {}.", user.getId(), voteType.name(), name.getId());
            } catch (DataAccessException ex) {
                LOGGER.warn("Failed to cast vote. User {} could not change to {} vote for name {}. Reason: {}.", user.getId(), voteType.name(), name.getId(), ex.getMessage());
                throw new VoteException(ex.getMessage());
            }
        }
    }

    private static Vote mapper(ResultSet resultSet, int index) throws SQLException {
        return new Vote(
                resultSet.getString("user_id"),
                resultSet.getString("name_id"),
                VoteType.valueOf(resultSet.getString("type")));
    }
}
