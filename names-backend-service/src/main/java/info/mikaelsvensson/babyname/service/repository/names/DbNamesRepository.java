package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.*;
import info.mikaelsvensson.babyname.service.model.name.MetricsProperties;
import info.mikaelsvensson.babyname.service.model.name.Name;
import info.mikaelsvensson.babyname.service.model.name.ScbProperties;
import info.mikaelsvensson.babyname.service.model.name.VotesProperties;
import info.mikaelsvensson.babyname.service.repository.names.request.*;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Repository
@Service
public class DbNamesRepository implements NamesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbNamesRepository.class);

    private ApplicationEventPublisher applicationEventPublisher;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private Map<String, String> cacheAllNames;

    public DbNamesRepository(@Autowired ApplicationEventPublisher applicationEventPublisher,
                             @Autowired NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public void all(NamesRequest request, Consumer<Name> nameConsumer) throws NameException {
        find(request, nameConsumer);
    }

    @Override
    public synchronized Map<String, String> allNames() throws NameException {
        if (cacheAllNames == null) {
            var tempMap = new HashMap<String, String>();
            all(new NamesRequest().basic(new BasicNameFacet()), name -> {
                tempMap.put(name.getId(), name.getName());
            });
            LOGGER.info("allNames: Cached {} names.", tempMap.size());
            cacheAllNames = Map.copyOf(tempMap);
        } else {
            LOGGER.info("allNames: Returned {} names.", cacheAllNames.size());
        }
        return cacheAllNames;
    }

    @Override
    public synchronized Name add(String name, User user) throws NameException {
        try {
            final var obj = new Name(
                    name,
                    IdUtils.random()
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

            applicationEventPublisher.publishEvent(new NameAddedEvent(obj, this));

            cacheAllNames = null;
            return obj;
        } catch (DataAccessException e) {
            throw new NameException(e.getMessage());
        }
    }

    private static class NamesResultSetIterator implements ResultSetExtractor<Integer> {
        private final Consumer<Name> nameConsumer;
        private final NamesRequest request;


        public NamesResultSetIterator(Consumer<Name> nameConsumer, NamesRequest request) {
            this.nameConsumer = nameConsumer;
            this.request = request;
        }

        @Override
        public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
            var returnedNames = 0;
            while (rs.next()) {
                final var rsId = rs.getString("n__id");
                final var rsName = rs.getString("n__name");
                final var currentName = new Name(
                        rsName,
                        rsId
                );

                if (request.basic != null && request.basic.returned) {
                }
                if (request.metrics != null && request.metrics.returned) {
                    currentName.setMetrics(new MetricsProperties(
                            getDouble(rs, "metrics__syllable_count").orElse(null)
                    ));
                }
                if (request.scb != null && request.scb.returned) {
                    currentName.setScb(new ScbProperties(
                            getDouble(rs, "scb__percent_of_population").orElse(null),
                            getDouble(rs, "scb__percent_women").orElse(null)
                    ));
                }
                if (request.votes != null && request.votes.returned) {
                    currentName.setVotes(new VotesProperties(
                            getDouble(rs, "votes__self_vote_value").map(Double::intValue).orElse(null),
                            getDouble(rs, "votes__partner_vote_value").map(Double::intValue).orElse(null)
                    ));
                }

                returnedNames++;
                nameConsumer.accept(currentName);
            }
            return returnedNames;
        }

        private Optional<Double> getDouble(ResultSet rs, String column) throws SQLException {
            final var value = rs.getDouble(column);
            if (rs.wasNull()) {
                return Optional.empty();
            }
            return Optional.of(value);
        }
    }


    private void find(NamesRequest request, Consumer<Name> nameConsumer) throws NameException {
        try {
            final var params = new HashMap<String, Object>();

            final var sqlWhere = new HashSet<String>();
            sqlWhere.add("TRUE");
            final var sqlSelect = new HashSet<String>();
            final var sqlLeftJoins = new LinkedHashSet<String>();
            sqlLeftJoins.add("names AS n");

            if (request.basic != null && request.basic.returned) {
                sqlSelect.add("n.id AS n__id");
                sqlSelect.add("n.name AS n__name");
            }
            if (request.metrics != null && request.metrics.returned) {
                sqlSelect.add("metrics.syllable_count AS metrics__syllable_count");
                sqlLeftJoins.add("names_metrics AS metrics ON n.id = metrics.name_id ");
            }
            if (request.scb != null && request.scb.returned) {
                sqlSelect.add("scb.percent_of_population AS scb__percent_of_population");
                sqlSelect.add("scb.percent_women AS scb__percent_women");
                sqlLeftJoins.add("names_scb AS scb ON n.id = scb.name_id ");
            }
            if (request.votes != null && request.votes.returned) {
                if (request.votes.selfUserId != null) {
                    sqlLeftJoins.add(MessageFormat.format("votes AS votes_{0} ON n.id = votes_{0}.name_id AND votes_{0}.user_id = ''{0}''", request.votes.selfUserId));
                    sqlSelect.add(MessageFormat.format("votes_{0}.value AS votes__self_vote_value", request.votes.selfUserId));
                }
                if (request.votes.partnerUserId != null) {
                    sqlLeftJoins.add(MessageFormat.format("votes AS votes_{0} ON n.id = votes_{0}.name_id AND votes_{0}.user_id = ''{0}''", request.votes.partnerUserId));
                    sqlSelect.add(MessageFormat.format("votes_{0}.value AS votes__partner_vote_value", request.votes.partnerUserId));
                }
            }

            if (sqlSelect.isEmpty()) {
                throw new NameException("At least one facet must be returned.");
            }

            if (request.basic != null && request.basic.nameId != null) {
                sqlWhere.add("n.id = :nameId");
                params.put("nameId", request.basic.nameId);
            }
            if (request.basic != null && request.basic.nameExact != null) {
                sqlWhere.add("n.name = :nameExact");
                params.put("nameExact", request.basic.nameExact);
            }
            if (request.basic != null && request.basic.namePrefix != null) {
                sqlWhere.add("LOWER(n.name) LIKE :namePrefix");
                params.put("namePrefix", request.basic.namePrefix.toLowerCase() + "%");
            }
            if (request.basic != null && request.basic.nameOwnerUserIds != null && !request.basic.nameOwnerUserIds.isEmpty()) {
                sqlWhere.add("EXISTS (SELECT 1 FROM name_owners AS no WHERE n.id = no.name_id AND no.user_id IN (:nameOwners))");
                params.put("nameOwners", request.basic.nameOwnerUserIds);
            }
            if (request.metrics != null && !request.metrics.syllableFilter.isEmpty()) {
                for (FilterAttributeNumeric filter : request.metrics.syllableFilter) {
                    var operator = switch (filter.getOperator()) {
                        case LESS_THAN -> '<';
                        case GREATER_THAN -> '>';
                    };
                    sqlLeftJoins.add("names_metrics AS metrics ON n.id = metrics.name_id ");
                    sqlWhere.add(MessageFormat.format("metrics.syllable_count {0} :syllableFilter{1}", operator, params.size()));
                    params.put("syllableFilter" + params.size(), filter.getValue());
                }
            }
            if (request.scb != null && !request.scb.percentOfPopulationFilter.isEmpty()) {
                for (FilterAttributeNumeric filter : request.scb.percentOfPopulationFilter) {
                    var operator = switch (filter.getOperator()) {
                        case LESS_THAN -> '<';
                        case GREATER_THAN -> '>';
                    };
                    sqlLeftJoins.add("names_scb AS scb ON n.id = scb.name_id ");
                    sqlWhere.add(MessageFormat.format("scb.percent_of_population {0} :percentOfPopulationFilter{1}", operator, params.size()));
                    params.put("percentOfPopulationFilter" + params.size(), filter.getValue());
                }
            }
            if (request.scb != null && request.scb.percentWomenFilter != null) {
                for (FilterAttributeNumeric filter : request.scb.percentWomenFilter) {
                    var operator = switch (filter.getOperator()) {
                        case LESS_THAN -> '<';
                        case GREATER_THAN -> '>';
                    };
                    sqlLeftJoins.add("names_scb AS scb ON n.id = scb.name_id ");
                    sqlWhere.add(MessageFormat.format("scb.percent_women {0} :percentWomenFilter{1}", operator, params.size()));
                    params.put("percentWomenFilter" + params.size(), filter.getValue());
                }
            }
            if (request.votes != null && request.votes.filterVotes != null && !request.votes.filterVotes.isEmpty()) {
                for (FilterVote filterVote : request.votes.filterVotes) {
                    if (filterVote.getUserIds().isEmpty()) {
                        throw new NameException("List of user ids cannot be empty.");
                    }
                    for (String userId : filterVote.getUserIds()) {
                        sqlLeftJoins.add(MessageFormat.format("votes AS votes_{0} ON n.id = votes_{0}.name_id AND votes_{0}.user_id = ''{0}''", userId));
                    }
                    final var sqlOrStatements = filterVote.getUserIds().stream()
                            .map(userId -> MessageFormat.format(
                                    "votes_{0}.value {1}",
                                    userId,
                                    switch (filterVote.getCondition()) {
                                        case NOT_YET_VOTED -> " IS NULL";
                                        case ANY_VOTE -> "      IS NOT NULL";
                                        case POSITIVE_VOTE -> " > 0";
                                    }))
                            .collect(Collectors.joining(" OR "));

                    sqlWhere.add(MessageFormat.format("({0})", sqlOrStatements));
                }
            }

            params.put("limit", request.limit);
            params.put("offset", request.offset);
            final var query = "" +
                    "\n SELECT " +
                    "\n     " + String.join(", ", sqlSelect) +
                    "\n FROM " +
                    "\n     " + String.join(" LEFT JOIN \n     ", sqlLeftJoins) +
                    "\n WHERE " +
                    "\n     " + String.join(" AND \n     ", sqlWhere) +
                    "\n ORDER BY " +
                    "\n    n.name " +
                    "\n LIMIT " +
                    "\n    :limit " +
                    "\n OFFSET " +
                    "\n    :offset ";
            namedParameterJdbcTemplate.query(query,
                    params,
                    new NamesResultSetIterator(nameConsumer, request));
        } catch (DataAccessException e) {
            LOGGER.warn("Failed to query database.", e);
            throw new NameException(e.getMessage());
        }
    }

    @Override
    public Name get(String nameId, User user) throws NameException {
        var res = new Name[]{null};
        final var request = new NamesRequest()
                .basic(new BasicNameFacet().nameId(nameId))
                .scb(new ScbNameFacet())
                .metrics(new MetricsNameFacet());
        if (user != null) {
            request.votes(new VotesNameFacet()
                    .selfUserId(user.getId())
                    .partnerUserId(user.getRelatedUserId()));
        }
        find(request, name -> res[0] = name);
        if (res[0] != null) {
            return res[0];
        } else {
            throw new NameException("Could not find name");
        }
    }

    @Override
    public Optional<Name> getByName(String name) throws NameException {
        var res = new Name[]{null};
        find(new NamesRequest().basic(new BasicNameFacet().nameExact(name)).scb(new ScbNameFacet()), n -> res[0] = n);
        if (res[0] != null) {
            return Optional.of(res[0]);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void setScbProperties(Name name, ScbProperties properties) throws NameException {
        final var sqlParams = new MapSqlParameterSource()
                .addValue("nameId", name.getId())
                .addValue("percentOfPopulation", properties.getPercentOfPopulation())
                .addValue("percentWomen", properties.getPercentWomen());
        try {
            var rowsInserted = namedParameterJdbcTemplate.update("INSERT INTO names_scb (name_id, percent_of_population, percent_women) VALUES (:nameId, :percentOfPopulation, :percentWomen)", sqlParams);
            if (rowsInserted != 1) {
                throw new NameException(MessageFormat.format("Could not set metrics for name {0}. {1} rows inserted.", name.getId(), rowsInserted));
            }
        } catch (DataAccessException e) {
            var rowsUpdated = namedParameterJdbcTemplate.update("UPDATE names_scb SET percent_of_population = :percentOfPopulation, percent_women = :percentWomen WHERE name_id = :nameId", sqlParams);
            if (rowsUpdated != 1) {
                throw new NameException(MessageFormat.format("Could not set metrics for name {0}. {1} rows updaded.", name.getId(), rowsUpdated));
            }
        }

    }

    @Override
    public void setMetricsProperties(Name name, MetricsProperties properties) throws NameException {
        final var sqlParams = Map.of(
                "nameId", name.getId(),
                "syllableCount", properties.getSyllableCount()
        );
        try {
            var rowsInserted = namedParameterJdbcTemplate.update("INSERT INTO names_metrics (name_id, syllable_count) VALUES (:nameId, :syllableCount)", sqlParams);
            if (rowsInserted != 1) {
                throw new NameException(MessageFormat.format("Could not set metrics for name {0}. {1} rows inserted.", name.getId(), rowsInserted));
            }
        } catch (DataAccessException e) {
            var rowsUpdated = namedParameterJdbcTemplate.update("UPDATE names_metrics SET syllable_count = :syllableCount WHERE name_id = :nameId", sqlParams);
            if (rowsUpdated != 1) {
                throw new NameException(MessageFormat.format("Could not set metrics for name {0}. {1} rows updaded.", name.getId(), rowsUpdated));
            }
        }
    }
}
