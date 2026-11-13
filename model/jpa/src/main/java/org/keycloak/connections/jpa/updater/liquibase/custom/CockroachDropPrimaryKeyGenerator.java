package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import liquibase.Scope;
import liquibase.change.core.AddPrimaryKeyChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.core.CockroachDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.DropPrimaryKeyGenerator;
import liquibase.statement.core.DropPrimaryKeyStatement;

/**
 * CockroachDB rejects dropping a primary key without adding a new one in the same statement, and doesn't allow dropping a column that is part of the primary key.
 * The primary key is therefore replaced with {@code ALTER PRIMARY KEY} at the place of the drop, using the columns of the next {@code addPrimaryKey} for the same table in the changeset.
 * The {@code addPrimaryKey} is then skipped by {@link CockroachAddPrimaryKeyGenerator}.
 * <p>
 * CockroachDB keeps the old primary key as a unique secondary index, which PostgreSQL wouldn't have and which would reject valid rows (for example multi-valued user attributes).
 * That index is dropped right after, using the name CockroachDB assigns to it: {@code <table>_<column>_<column>_key}.
 */
public class CockroachDropPrimaryKeyGenerator extends DropPrimaryKeyGenerator {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE + 100;
    }

    @Override
    public boolean supports(DropPrimaryKeyStatement statement, Database database) {
        return database instanceof CockroachDatabase;
    }

    @Override
    public Sql[] generateSql(DropPrimaryKeyStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        Optional<AddPrimaryKeyChange> add = findAdd(statement.getTableName());
        if (add.isEmpty()) {
            return new Sql[0];
        }
        String table = database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName());
        String columns = database.escapeColumnNameList(add.get().getColumnNames());
        List<String> oldColumns = currentPrimaryKeyColumns(database, statement.getTableName());
        String oldIndex = (statement.getTableName() + "_" + String.join("_", oldColumns) + "_key").toLowerCase();
        return new Sql[] {
                new UnparsedSql("ALTER TABLE " + table + " ALTER PRIMARY KEY USING COLUMNS (" + columns + ")"),
                new UnparsedSql("DROP INDEX IF EXISTS " + table + "@" + oldIndex)
        };
    }

    private static List<String> currentPrimaryKeyColumns(Database database, String tableName) {
        Connection connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        String sql = "SELECT kcu.column_name FROM information_schema.table_constraints tc"
                + " JOIN information_schema.key_column_usage kcu ON kcu.constraint_name = tc.constraint_name"
                + " AND kcu.table_schema = tc.table_schema AND kcu.table_name = tc.table_name"
                + " WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_schema = current_schema() AND lower(tc.table_name) = lower(?)"
                + " ORDER BY kcu.ordinal_position";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> columns = new ArrayList<>();
                while (rs.next()) {
                    columns.add(rs.getString(1));
                }
                return columns;
            }
        } catch (SQLException e) {
            throw new UnexpectedLiquibaseException("Unable to read primary key of " + tableName, e);
        }
    }

    static Optional<AddPrimaryKeyChange> findAdd(String tableName) {
        ChangeSet changeSet = Scope.getCurrentScope().get(Scope.Attr.changeSet, ChangeSet.class);
        if (changeSet == null) {
            return Optional.empty();
        }
        return changeSet.getChanges().stream()
                .filter(c -> c instanceof AddPrimaryKeyChange add && tableName.equalsIgnoreCase(add.getTableName()))
                .map(c -> (AddPrimaryKeyChange) c)
                .findFirst();
    }

}
