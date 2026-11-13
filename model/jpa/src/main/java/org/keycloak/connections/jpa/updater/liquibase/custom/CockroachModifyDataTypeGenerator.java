package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.util.Arrays;
import java.util.regex.Pattern;

import liquibase.database.Database;
import liquibase.database.core.CockroachDatabase;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.ModifyDataTypeGenerator;
import liquibase.statement.core.ModifyDataTypeStatement;
import liquibase.structure.DatabaseObject;

/**
 * The statement generated for PostgreSQL contains a {@code USING} clause, which CockroachDB treats as a rewrite of the column.
 * That is not supported for columns that are part of an index, while changing a column to a string type without {@code USING} is a metadata change only.
 * Conversions to other types still need the clause.
 */
public class CockroachModifyDataTypeGenerator extends ModifyDataTypeGenerator {

    private static final Pattern USING = Pattern.compile("\\s+USING\\s*\\(.*\\)\\s*$", Pattern.DOTALL);

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE + 100;
    }

    @Override
    public boolean supports(ModifyDataTypeStatement statement, Database database) {
        return database instanceof CockroachDatabase;
    }

    @Override
    public Sql[] generateSql(ModifyDataTypeStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        Sql[] sql = super.generateSql(statement, database, sqlGeneratorChain);
        String type = statement.getNewDataType().toUpperCase();
        if (!type.contains("CHAR") && !type.contains("TEXT") && !type.contains("STRING")) {
            return sql;
        }
        return Arrays.stream(sql)
                .map(s -> (Sql) new UnparsedSql(USING.matcher(s.toSql()).replaceFirst(""), s.getAffectedDatabaseObjects().toArray(new DatabaseObject[0])))
                .toArray(Sql[]::new);
    }

}
