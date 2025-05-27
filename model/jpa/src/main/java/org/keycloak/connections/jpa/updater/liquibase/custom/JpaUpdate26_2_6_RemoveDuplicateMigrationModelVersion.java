package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.DeleteStatement;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.Column;
import liquibase.structure.core.Schema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Cleanup script for removing duplicated migration model versions in the MIGRATION_MODEL table
 * See: <a href="https://github.com/keycloak/keycloak/issues/39866">keycloak#39866</a>
 */
public class JpaUpdate26_2_6_RemoveDuplicateMigrationModelVersion extends CustomKeycloakTask {

    @Override
    protected String getTaskId() {
        return "Delete duplicated records for DB version in MIGRATION_MODEL table";
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        Set<String> idsToDelete = new HashSet<>();

        final String catalog = database.escapeObjectName(database.getDefaultCatalogName(), Catalog.class);
        final String schema = getSchema();
        final String tableName = getTableName("MIGRATION_MODEL");
        final String colId = database.correctObjectName("ID", Column.class);

        final String GET_OLDER_DUPLICATED_RECORDS = """
                SELECT m1.ID
                FROM %sMIGRATION_MODEL m1
                WHERE EXISTS (
                    SELECT m2.ID
                    FROM %sMIGRATION_MODEL m2
                    WHERE m2.VERSION = m1.VERSION AND m2.UPDATE_TIME > m1.UPDATE_TIME
                )
                """.formatted(schema, schema);

        try (PreparedStatement ps = connection.prepareStatement(GET_OLDER_DUPLICATED_RECORDS)) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                idsToDelete.add(resultSet.getString(1));
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Failed to detect duplicate MIGRATION_MODEL rows", e);
        }

        AtomicInteger i = new AtomicInteger();
        idsToDelete.stream()
                .collect(Collectors.groupingByConcurrent(id -> i.getAndIncrement() / 20, Collectors.toList())) // Split into chunks of at most 20 items
                .values().stream()
                .map(ids -> new DeleteStatement(catalog, schema, tableName)
                        .setWhere(":name IN (" + ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")")
                        .addWhereColumnName(colId)
                        .addWhereParameters(ids.toArray())
                )
                .forEach(statements::add);
    }

    private String getSchema() {
        String schema = database.escapeObjectName(database.getDefaultSchemaName(), Schema.class);
        return schema == null ? "" : schema + ".";
    }
}
