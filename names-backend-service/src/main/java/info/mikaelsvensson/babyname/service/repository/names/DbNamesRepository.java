package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;

@Repository
@Service
public class DbNamesRepository implements NamesRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<Name> all(Set<String> userIds, String namePrefix, int limit, Set<String> voteUserIds, Set<AttributeFilterNumeric> numericFilters) throws NameException {
        return find(userIds, namePrefix, limit, voteUserIds, null, numericFilters);
    }

    @Override
    public Name add(String name, String ownerUserId, Set<Attribute<?>> attributes) throws NameException {
        try {
            final var obj = new Name(
                    name,
                    IdUtils.random(),
                    attributes
            );
            namedParameterJdbcTemplate.update(
                    "INSERT INTO names (id, name) VALUES (:id, :name)",
                    Map.of(
                            "id", obj.getId(),
                            "name", obj.getName()
                    ));
            namedParameterJdbcTemplate.update(
                    "INSERT INTO name_owners (name_id, user_id, created_at) VALUES (:id, :createdBy, :createdAt)",
                    Map.of(
                            "id", obj.getId(),
                            "createdBy", ownerUserId,
                            "createdAt", Instant.now().toEpochMilli()
                    ));
            return obj;
        } catch (DataAccessException e) {
            throw new NameException(e.getMessage());
        }
    }

    private List<Name> find(Set<String> userIds, String namePrefix, int limit, Set<String> voteUserIds, String nameId, Set<AttributeFilterNumeric> numericFilters) throws NameException {
        try {
            final var params = new HashMap<String, Object>();
            final var sqlWhere = new StringBuilder("TRUE");
            if (nameId != null) {
                sqlWhere.append(" AND n.id = :nameId");
                params.put("nameId", nameId);
            }
            if (namePrefix != null) {
                sqlWhere.append(" AND LOWER(n.name) LIKE :namePrefix");
                params.put("namePrefix", namePrefix.toLowerCase() + "%");
            }
            if (voteUserIds != null && !voteUserIds.isEmpty()) {
                sqlWhere.append(" AND n.id IN (SELECT v.name_id FROM votes AS v WHERE v.user_id IN (:voteUserIds))");
                params.put("voteUserIds", voteUserIds);
            }
            if (userIds != null && !userIds.isEmpty()) {
                sqlWhere.append(" AND no.user_id IN (:createdBy)");
                sqlWhere.append(" AND naf.created_by IN (:createdBy)");
            }
            if (numericFilters != null && !numericFilters.isEmpty()) {
                var i = 0;
                for (AttributeFilterNumeric numericFilter : numericFilters) {
                    i++;
                    var operator = switch (numericFilter.getOperator()) {
                        case LESS_THAN -> '<';
                        case GREATER_THAN -> '>';
                    };
                    sqlWhere.append(MessageFormat.format(" AND n.id IN (SELECT naf{0}.name_id FROM name_attributes_float AS naf{0} WHERE naf{0}.key = :numFilterKey{0} AND naf{0}.value {1} :numFilterValue{0})", i, operator));
                    params.put("numFilterKey" + i, numericFilter.getKey().name());
                    params.put("numFilterValue" + i, numericFilter.getValue());
                }
            }

            params.put("createdBy", userIds);

            var result = new ArrayList<Name>();
            namedParameterJdbcTemplate.query("" +
                            "SELECT " +
                            "   n.* " +
                            "   ,naf.key AS naf_key " +
                            "   ,naf.value AS naf_value " +
                            "FROM " +
                            "   names AS n " +
                            "       INNER JOIN name_owners AS no ON n.id = no.name_id " +
                            "       LEFT JOIN name_attributes_float AS naf ON n.id = naf.name_id " +
                            "WHERE " +
                            "   (" + sqlWhere.toString() + ") " +
                            "ORDER BY " +
                            "   n.name " +
                            "   ,naf.key ",
                    params,
                    rs -> {
                        Name currentName = null;
                        while (rs.next()) {
                            var rsId = rs.getString("id");
                            final String rsName = rs.getString("name");
                            if (currentName == null || !rsName.equals(currentName.getName())) {
                                if (result.size() > limit) {
                                    break;
                                }
                                currentName = new Name(
                                        rsName,
                                        rsId,
                                        new HashSet<>()
                                );
                                result.add(currentName);
                            }
                            if (rs.getString("naf_key") != null) {
                                currentName.addAttribute(new AttributeNumeric(
                                        AttributeKey.valueOf(rs.getString("naf_key")),
                                        rs.getDouble("naf_value")
                                ));
                            }
                        }
                    });
            return result;
        } catch (DataAccessException e) {
            throw new NameException(e.getMessage());
        }
    }

    @Override
    public Name get(String nameId) throws NameException {
        return find(null, null, 1, null, nameId, null).stream().findFirst().orElseThrow(() -> new NameException("Could not find name"));
    }

    @Override
    public void setNumericAttribute(Name name, User attributeOwner, AttributeKey key, Double value) throws NameException {
        Objects.requireNonNull(name);
        Objects.requireNonNull(attributeOwner);
        Objects.requireNonNull(key);
        if (value != null) {
            try {
                var rowsInserted = namedParameterJdbcTemplate.update(
                        "INSERT INTO name_attributes_float (name_id, key, value, created_by, created_at) VALUES (:nameId, :key, :value, :createdBy, :createdAt)",
                        Map.of(
                                "nameId", name.getId(),
                                "key", key.name(),
                                "value", value,
                                "createdBy", attributeOwner.getId(),
                                "createdAt", Instant.now().toEpochMilli()
                        ));
                if (rowsInserted == 0) {
                    throw new NameException(rowsInserted + " row(s) inserted.");
                }
            } catch (DataAccessException e) {
                var rowsUpdated = namedParameterJdbcTemplate.update(
                        "UPDATE name_attributes_float SET value = :value WHERE name_id = :nameId AND key = :key AND created_by = :createdBy",
                        Map.of(
                                "nameId", name.getId(),
                                "key", key.name(),
                                "value", value,
                                "createdBy", attributeOwner.getId()
                        ));
                if (rowsUpdated == 0) {
                    throw new NameException(rowsUpdated + " row(s) updated.");
                }
            }
        } else {
            try {
                var rowsInserted = namedParameterJdbcTemplate.update(
                        "DELETE FROM name_attributes_float WHERE name_id = :nameId AND key = :key AND created_by = :createdBy",
                        Map.of(
                                "nameId", name.getId(),
                                "key", key.name(),
                                "createdBy", attributeOwner.getId()
                        ));
                if (rowsInserted != 1) {
                    throw new NameException(rowsInserted + " row(s) deleted.");
                }
            } catch (DataAccessException e) {
                throw new NameException(e.getMessage());
            }
        }
    }
}
