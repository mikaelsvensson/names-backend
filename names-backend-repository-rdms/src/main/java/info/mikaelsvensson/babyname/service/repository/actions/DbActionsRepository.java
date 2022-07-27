package info.mikaelsvensson.babyname.service.repository.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.mikaelsvensson.babyname.service.model.Action;
import info.mikaelsvensson.babyname.service.model.ActionStatus;
import info.mikaelsvensson.babyname.service.model.ActionType;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Repository
@Service
@Profile("db-rdms")
public class DbActionsRepository implements ActionsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbActionsRepository.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public DbActionsRepository(
            @Autowired NamedParameterJdbcTemplate namedParameterJdbcTemplate
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Action get(String id) throws ActionException {
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT * FROM actions WHERE id = :id",
                    Map.of(
                            "id", id
                    ),
                    DbActionsRepository::mapper);
        } catch (EmptyResultDataAccessException e) {
            throw new ActionNotFoundException("Could not find action " + id);
        } catch (DataAccessException e) {
            LOGGER.info("Could not get action from database.", e);
            throw new ActionException(e.getMessage());
        }
    }

    private static Action mapper(ResultSet resultSet, int index) throws SQLException {
        return new Action(
                resultSet.getString("id"),
                ActionType.valueOf(resultSet.getString("action")),
                resultSet.getString("created_by"),
                ActionStatus.valueOf(resultSet.getString("status")),
                deserializeParameters(resultSet.getString("parameters"))
        );
    }

    @Override
    public Action add(User creator, ActionType actionType, Map<String, String> actionParameters, ActionStatus status) throws ActionException {
        try {
            final var obj = new Action(
                    IdUtils.random(),
                    actionType,
                    creator.getId(),
                    status,
                    actionParameters);
            namedParameterJdbcTemplate.update(
                    "INSERT INTO actions (id, created_by, action, parameters, status, created_at) VALUES (:id, :createdBy, :action, :parameters, :status, :createdAt)",
                    Map.of(
                            "id", obj.getId(),
                            "createdBy", obj.getCreatedBy(),
                            "action", obj.getType().name(),
                            "parameters", serializeParameters(actionParameters),
                            "status", obj.getStatus().name(),
                            "createdAt", Instant.now().toEpochMilli()
                    ));
            return obj;
        } catch (DataAccessException e) {
            throw new ActionException(e.getMessage());
        }
    }

    @Override
    public void setStatus(Action action, ActionStatus newStatus) throws ActionException {
        try {
            final var updatedRows = namedParameterJdbcTemplate.update(
                    "UPDATE actions SET status = :status WHERE id = :id",
                    Map.of(
                            "id", action.getId(),
                            "status", newStatus.name()
                    ));
            if (updatedRows != 1) {
                throw new ActionException("Updated " + updatedRows + " actions instead of one.");
            }
        } catch (DataAccessException e) {
            throw new ActionException(e.getMessage());
        }

    }

    private String serializeParameters(Map<String, String> actionParameters) {
        try {
            return OBJECT_MAPPER.writeValueAsString(actionParameters);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static Map<String, String> deserializeParameters(String parameters) {
        try {
            return OBJECT_MAPPER.readValue(parameters, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}
