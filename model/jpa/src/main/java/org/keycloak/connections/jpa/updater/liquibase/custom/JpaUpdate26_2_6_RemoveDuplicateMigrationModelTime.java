package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.keycloak.migration.ModelVersion;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.DeleteStatement;
import liquibase.structure.core.Column;

/**
 * Cleanup script for removing duplicated migration model update time in the MIGRATION_MODEL table
 * See: <a href="https://github.com/keycloak/keycloak/issues/40088">keycloak#40088</a>
 */
public class JpaUpdate26_2_6_RemoveDuplicateMigrationModelTime extends CustomKeycloakTask {
    private final static String MIGRATION_MODEL_TABLE = "MIGRATION_MODEL";

    @Override
    protected String getTaskId() {
        return "Delete duplicated records for DB update time in MIGRATION_MODEL table";
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        final Map<String, ModelVersion> itemsToDelete = new HashMap<>();

        final String tableName = getTableName(MIGRATION_MODEL_TABLE);
        final String colId = database.correctObjectName("ID", Column.class);
        final String colVersion = database.correctObjectName("VERSION", Column.class);
        final String colUpdateTime = database.correctObjectName("UPDATE_TIME", Column.class);

        final String GET_DUPLICATED_RECORDS = """
                SELECT m1.%s, m1.%s
                FROM %s m1
                WHERE EXISTS (
                    SELECT m2.%s
                    FROM %s m2
                    WHERE m2.%s = m1.%s AND m2.%s <> m1.%s
                )
                """.formatted(
                colId, colVersion,  // SELECT m1.%s, m1.%s  =>  SELECT m1.ID, m1.VERSION
                tableName,          // FROM %s m1           =>  FROM MIGRATION_MODEL m1
                colId,              // SELECT m2.%s         =>  SELECT m2.ID
                tableName,          // FROM %s m2           =>  FROM MIGRATION_MODEL m2
                // WHERE m2.%s = m1.%s AND m2.%s <> m1.%s   =>  WHERE m2.UPDATE_TIME = m1.UPDATE_TIME AND m2.ID <> m1.ID
                colUpdateTime, colUpdateTime, colId, colId
        );

        //noinspection SqlSourceToSinkFlow
        try (PreparedStatement ps = connection.prepareStatement(GET_DUPLICATED_RECORDS)) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String id = resultSet.getString(1);
                ModelVersion version = new ModelVersion(resultSet.getString(2));
                itemsToDelete.put(id, version);
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Failed to detect duplicate MIGRATION_MODEL rows", e);
        }

        // Get ID of the highest Keycloak version with the same update time
        var highestVersionId = itemsToDelete.entrySet()
                .stream()
                .reduce((e1, e2) -> e1.getValue().lessThan(e2.getValue()) ? e2 : e1)
                .map(Map.Entry::getKey)
                .orElse(null);

        AtomicInteger i = new AtomicInteger();
        itemsToDelete.keySet().stream()
                .filter(f -> !f.equals(highestVersionId))
                .collect(Collectors.groupingByConcurrent(id -> i.getAndIncrement() / 20, Collectors.toList())) // Split into chunks of at most 20 items
                .values().stream()
                .map(ids -> new DeleteStatement(null, null, MIGRATION_MODEL_TABLE)
                        .setWhere(":name IN (" + ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")")
                        .addWhereColumnName(colId)
                        .addWhereParameters(ids.toArray())
                )
                .forEach(statements::add);
    }
}
