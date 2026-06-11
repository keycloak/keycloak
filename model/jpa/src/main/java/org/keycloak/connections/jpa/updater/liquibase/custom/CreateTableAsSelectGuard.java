package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.util.regex.Pattern;

import liquibase.database.Database;
import liquibase.database.core.MySQLDatabase;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.core.RawSqlStatement;

/**
 * Rejects {@code CREATE TABLE ... AS SELECT} on MySQL — incompatible with Group Replication (ERROR 3098).
 * Only raw SQL from {@link CustomKeycloakTask} subclasses needs guarding, as XML changelogs cannot express this pattern.
 */
public class CreateTableAsSelectGuard extends AbstractSqlGenerator<RawSqlStatement> {

    private static final Pattern CTAS_PATTERN = Pattern.compile("(?i)CREATE\\s+TABLE\\s+\\S+\\s+AS\\s+SELECT");

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE + 100;
    }

    @Override
    public boolean supports(RawSqlStatement statement, Database database) {
        return database instanceof MySQLDatabase;
    }

    @Override
    public ValidationErrors validate(RawSqlStatement statement, Database database, SqlGeneratorChain<RawSqlStatement> chain) {
        return new ValidationErrors();
    }

    @Override
    public Sql[] generateSql(RawSqlStatement statement, Database database, SqlGeneratorChain<RawSqlStatement> chain) {
        String sql = statement.getSql().trim();
        if (CTAS_PATTERN.matcher(sql).find()) {
            throw new RuntimeException("CREATE TABLE ... AS SELECT is incompatible with MySQL Group Replication "
                    + "(ERROR 3098). Use CREATE TABLE with inline PRIMARY KEY followed by INSERT INTO ... SELECT. "
                    + "Found: " + sql);
        }
        return chain.generateSql(statement, database);
    }
}
