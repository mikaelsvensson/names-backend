package info.mikaelsvensson.babyname.service.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class DbRepositoryHealthcheck implements RepositoryHealthcheck {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DbRepositoryHealthcheck(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void ping() throws Exception {
        final var isResultReturned = jdbcTemplate.queryForRowSet("SELECT 1", Collections.emptyMap()).next();
        if (!isResultReturned) {
            throw new Exception("Could not ping database (no result returned)");
        }
    }
}
