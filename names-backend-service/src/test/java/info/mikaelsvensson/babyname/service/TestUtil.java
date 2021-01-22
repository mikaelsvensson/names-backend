package info.mikaelsvensson.babyname.service;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.SQLException;

public class TestUtil {
    public static NamedParameterJdbcTemplate createJdbcTemplate() throws LiquibaseException, SQLException {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("unit-test;MODE=PostgreSQL")
                .build();

        Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), new JdbcConnection(dataSource.getConnection()));
        liquibase.update("");

        return new NamedParameterJdbcTemplate(dataSource);
    }
}
