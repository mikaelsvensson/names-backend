package info.mikaelsvensson.babyname.service.repository.relationships;

import info.mikaelsvensson.babyname.service.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Repository
@Service
@Profile("db-rdms")
public class DbRelationshipsRepository implements RelationshipsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbRelationshipsRepository.class);

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final ApplicationEventPublisher applicationEventPublisher;

    public DbRelationshipsRepository(
            @Autowired NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Autowired ApplicationEventPublisher applicationEventPublisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void connect(User user, User relatedUser) throws RelationshipException {
        try {
            namedParameterJdbcTemplate.update(
                    "DELETE FROM relationships WHERE user_id IN (:userId, :relatedUserId) OR related_user_id IN (:userId, :relatedUserId)",
                    Map.of(
                            "userId", user.getId(),
                            "relatedUserId", relatedUser.getId()
                    ));
            namedParameterJdbcTemplate.update(
                    "INSERT INTO relationships (user_id, related_user_id) VALUES (:userId, :relatedUserId), (:relatedUserId, :userId)",
                    Map.of(
                            "userId", user.getId(),
                            "relatedUserId", relatedUser.getId()
                    ));
            LOGGER.info("Linked {} and {}.", user.getId(), relatedUser.getId());
            applicationEventPublisher.publishEvent(new RelationshipAddedEvent(this));
        } catch (DataAccessException e) {
            throw new RelationshipException(e.getMessage());
        }

    }
}
