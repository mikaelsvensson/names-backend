package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.Name;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@Service
@Profile("db")
public class DbNamesRepository implements NamesRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<Name> all(Set<String> userIds, String namePrefix, int limit, CountRange countRange) throws NameException {
        try {
            final var params = new HashMap<String, Object>();
            final var sqlWhere = new StringBuilder("FALSE");
            if (namePrefix != null) {
                sqlWhere.append(" OR LOWER(n.name) LIKE :namePrefix");
                params.put("namePrefix", namePrefix.toLowerCase() + "%");
            }

            params.put("limit", limit);
            params.put("createdBy", userIds);
            return namedParameterJdbcTemplate.query("" +
                            "SELECT " +
                            "   n.* " +
                            "FROM " +
                            "   names AS n " +
                            "WHERE " +
                            "   (" + sqlWhere.toString() + ") " +
                            "   AND n.created_by IN (:createdBy) " +
                            "ORDER BY " +
                            "   n.name " +
                            "LIMIT " +
                            "   :limit ",
                    params,
                    DbNamesRepository::mapper);
        } catch (DataAccessException e) {
            throw new NameException(e.getMessage());
        }
    }

    @Override
    public Name add(String name, Boolean isMale, Boolean isFemale, boolean isPublic, String ownerUserId) throws NameException {
        try {
            final var obj = new Name(
                    name,
                    null,
                    isMale,
                    isFemale,
                    ownerUserId,
                    IdUtils.random(),
                    isPublic
            );
            namedParameterJdbcTemplate.update(
                    "INSERT INTO names (id, name, is_male, is_female, is_public, created_by, created_at) VALUES (:id, :name, :is_male, :is_female, :is_public, :created_by, :created_at)",
                    Map.of(
                            "id", obj.getId(),
                            "name", obj.getName(),
                            "is_male", obj.isMale(),
                            "is_female", obj.isFemale(),
                            "is_public", obj.isPublic(),
                            "created_by", obj.getOwnerUserId(),
                            "created_at", Instant.now().toEpochMilli()
                    ));
            return obj;
        } catch (DataAccessException e) {
            throw new NameException(e.getMessage());
        }
    }

    @Override
    public Name get(String nameId) throws NameException {
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT * FROM names WHERE id = :id",
                    Map.of(
                            "id", nameId
                    ),
                    DbNamesRepository::mapper);
        } catch (DataAccessException e) {
            throw new NameException(e.getMessage());
        }
    }

    private static Name mapper(ResultSet resultSet, int index) throws SQLException {
        return new Name(
                resultSet.getString("name"),
                null,
                resultSet.getBoolean("is_male"),
                resultSet.getBoolean("is_female"),
                resultSet.getString("created_by"),
                resultSet.getString("id"),
                resultSet.getBoolean("is_public"));
    }
}
