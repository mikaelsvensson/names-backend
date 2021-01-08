package info.mikaelsvensson.babyname.service.repository.relationships;

import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.repository.users.DbUserRepository;
import info.mikaelsvensson.babyname.service.util.metrics.MetricEvent;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Repository
@Service
public class DbRelationshipsRepository implements RelationshipsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbRelationshipsRepository.class);

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private Metrics metrics;

    @Override
    public void add(User user, User relatedUser) throws RelationshipException {
        try {
            namedParameterJdbcTemplate.update(
                    "INSERT INTO relationships (user_id, related_user_id) VALUES (:userId, :relatedUserId)",
                    Map.of(
                            "userId", user.getId(),
                            "relatedUserId", relatedUser.getId()
                    ));
            LOGGER.info("Linked {} and {}.", user.getId(), relatedUser.getId());
            metrics.logEvent(MetricEvent.RELATIONSHIP_ADDED);
        } catch (DataAccessException e) {
            throw new RelationshipException(e.getMessage());
        }

    }

    @Override
    public List<User> getRelatedUsers(User user) throws RelationshipException {
        try {
            return namedParameterJdbcTemplate.query(
                    "SELECT u.* FROM relationships AS r INNER JOIN users AS u ON r.related_user_id = u.id AND r.user_id = :id",
                    Map.of(
                            "id", user.getId()
                    ),
                    DbUserRepository::mapper);
        } catch (DataAccessException e) {
            throw new RelationshipException(e.getMessage());
        }
    }
}
