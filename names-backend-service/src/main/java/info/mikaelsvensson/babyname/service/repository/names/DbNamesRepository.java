package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import info.mikaelsvensson.babyname.service.util.NameFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

@Repository
@Service
public class DbNamesRepository implements NamesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbNamesRepository.class);

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private Map<String, String> cacheAllNames;

    public DbNamesRepository(@Autowired NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public void all(Set<String> userIds,
                    String namePrefix,
                    int offset,
                    int limit,
                    Set<String> voteUserIds,
                    Set<FilterAttributeNumeric> numericFilters,
                    Set<FilterVote> filterVotes,
                    Consumer<Name> nameConsumer
    ) throws NameException {
        find(new Condition()
                        .userIds(userIds)
                        .namePrefix(namePrefix)
                        .voteUserIds(voteUserIds)
                        .numericFilters(numericFilters)
                        .filterVotes(filterVotes),
                offset,
                limit,
                nameConsumer);
    }

    @Override
    public synchronized Map<String, String> allNames() throws NameException {
        if (cacheAllNames == null) {
            cacheAllNames = new HashMap<>();
            all(null, null, 0, Integer.MAX_VALUE, null, null, null, name -> {
                cacheAllNames.put(name.getId(), name.getName());
            });
            LOGGER.info("allNames: Cached {} names.", cacheAllNames.size());
        } else {
            LOGGER.info("allNames: Returned {} names.", cacheAllNames.size());
        }
        return cacheAllNames;
    }

    @Override
    public synchronized Name add(String name, User user, Set<Attribute<?>> attributes) throws NameException {
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
                            "createdBy", user.getId(),
                            "createdAt", Instant.now().toEpochMilli()
                    ));
            for (Attribute<?> attribute : obj.getAttributes()) {
                if (attribute instanceof AttributeNumeric) {
                    AttributeNumeric attributeNumeric = (AttributeNumeric) attribute;
                    setNumericAttribute(obj, user, attributeNumeric.getKey(), attributeNumeric.getValue());
                }
            }

            setNumericAttribute(obj, user, AttributeKey.SYLLABLE_COUNT, (double) NameFeature.syllableCount(name));
            cacheAllNames = null;
            return obj;
        } catch (DataAccessException e) {
            throw new NameException(e.getMessage());
        }
    }

    private static class NamesResultSetIterator implements ResultSetExtractor<Integer> {
        private final Consumer<Name> nameConsumer;
        private final int offset;
        private final int limit;


        public NamesResultSetIterator(Consumer<Name> nameConsumer, int offset, int limit) {
            this.nameConsumer = nameConsumer;
            this.offset = offset;
            this.limit = limit;
        }

        @Override
        public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
            Name currentName = null;
            String prevRowName = null;
            var currentIndex = -1;
            var returnedNames = 0;
            while (rs.next()) {
                final var rsId = rs.getString("id");
                final var rsName = rs.getString("name");
                final var rsNafKey = rs.getString("naf_key");
                final var isNewName = !Objects.equals(rsName, prevRowName);
                prevRowName = rsName;
                if (isNewName) {
                    currentIndex++;
                    if (currentIndex < offset) {
                        continue;
                    }

                    if (currentName != null) {
                        returnedNames++;
                        nameConsumer.accept(currentName);

                        if (returnedNames == limit) {
                            return returnedNames;
                        }
                    }
                    currentName = new Name(
                            rsName,
                            rsId,
                            new HashSet<>()
                    );
                } else {
                    if (currentIndex < offset) {
                        continue;
                    }
                }
                if (rsNafKey != null) {
                    currentName.addAttribute(new AttributeNumeric(
                            AttributeKey.valueOf(rsNafKey),
                            rs.getDouble("naf_value")
                    ));
                }
            }
            if (currentName != null) {
                returnedNames++;
                nameConsumer.accept(currentName);
            }
            return returnedNames;
        }
    }


    private void find(Condition condition, int offset, int limit, Consumer<Name> nameConsumer) throws NameException {
        try {
            final var params = new HashMap<String, Object>();
            final var sqlWhere = new StringBuilder("TRUE");
            if (condition.nameId != null) {
                sqlWhere.append(" AND n.id = :nameId");
                params.put("nameId", condition.nameId);
            }
            if (condition.nameExact != null) {
                sqlWhere.append(" AND n.name = :nameExact");
                params.put("nameExact", condition.nameExact);
            }
            if (condition.namePrefix != null) {
                sqlWhere.append(" AND LOWER(n.name) LIKE :namePrefix");
                params.put("namePrefix", condition.namePrefix.toLowerCase() + "%");
            }
            if (condition.voteUserIds != null && !condition.voteUserIds.isEmpty()) {
                sqlWhere.append(" AND n.id IN (SELECT v.name_id FROM votes AS v WHERE v.user_id IN (:voteUserIds))");
                params.put("voteUserIds", condition.voteUserIds);
            }
            if (condition.userIds != null && !condition.userIds.isEmpty()) {
                sqlWhere.append(" AND no.user_id IN (:createdBy)");
                sqlWhere.append(" AND naf.created_by IN (:createdBy)");
                params.put("createdBy", condition.userIds);
            }
            if (condition.numericFilters != null && !condition.numericFilters.isEmpty()) {
                var i = 0;
                for (FilterAttributeNumeric numericFilter : condition.numericFilters) {
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
            if (condition.filterVotes != null && !condition.filterVotes.isEmpty()) {
                var i = 0;
                for (FilterVote filterVote : condition.filterVotes) {
                    if (filterVote.getUserIds().isEmpty()) {
                        throw new NameException("List of user ids cannot be empty.");
                    }
                    i++;
                    sqlWhere.append(MessageFormat.format(
                            switch (filterVote.getCondition()) {
                                case NOT_YET_VOTED -> " AND n.id NOT IN (SELECT vf{0}.name_id FROM votes AS vf{0} WHERE vf{0}.user_id IN (:voteFilterUsers{0}))";
                                case ANY_VOTE -> "      AND n.id     IN (SELECT vf{0}.name_id FROM votes AS vf{0} WHERE vf{0}.user_id IN (:voteFilterUsers{0}))";
                                case POSITIVE_VOTE -> " AND n.id     IN (SELECT vf{0}.name_id FROM votes AS vf{0} WHERE vf{0}.user_id IN (:voteFilterUsers{0}) AND vf{0}.value > 0)";
                            }, i));
                    params.put("voteFilterUsers" + i, filterVote.getUserIds());
                }
            }

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
                    new NamesResultSetIterator(nameConsumer, offset, limit));
        } catch (DataAccessException e) {
            throw new NameException(e.getMessage());
        }
    }

    @Override
    public Name get(String nameId) throws NameException {
        var res = new Name[]{null};
        find(new Condition().nameId(nameId), 0, 1, name -> res[0] = name);
        if (res[0] != null) {
            return res[0];
        } else {
            throw new NameException("Could not find name");
        }
    }

    @Override
    public Optional<Name> getByName(String name) throws NameException {
        var res = new Name[]{null};
        find(new Condition().nameExact(name), 0, 1, n -> res[0] = n);
        if (res[0] != null) {
            return Optional.of(res[0]);
        } else {
            return Optional.empty();
        }
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
