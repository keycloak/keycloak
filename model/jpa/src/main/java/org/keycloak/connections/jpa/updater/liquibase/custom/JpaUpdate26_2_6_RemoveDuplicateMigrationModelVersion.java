package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.DeleteStatement;
import liquibase.structure.core.Column;

/**
 * Cleanup script for removing duplicated migration model versions in the MIGRATION_MODEL table
 * See: <a href="https://github.com/keycloak/keycloak/issues/39866">keycloak#39866</a>
 */
public class JpaUpdate26_2_6_RemoveDuplicateMigrationModelVersion extends CustomKeycloakTask {

    private final static String MIGRATION_MODEL_TABLE = "MIGRATION_MODEL";

    @Override
    protected String getTaskId() {
        return "Delete duplicated records for DB version in MIGRATION_MODEL table";
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        Set<String> idsToDelete = new HashSet<>();

        final String tableName = getTableName(MIGRATION_MODEL_TABLE);
        final String colId = database.correctObjectName("ID", Column.class);
        final String colVersion = database.correctObjectName("VERSION", Column.class);
        final String colUpdateTime = database.correctObjectName("UPDATE_TIME", Column.class);

        //noinspection SqlSourceToSinkFlow
        try (PreparedStatement ps = connection.prepareStatement(getOlderDuplicatedRecords(tableName, colId, colVersion, colUpdateTime))) {
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
                .map(ids -> new DeleteStatement(null, null, MIGRATION_MODEL_TABLE)
                        .setWhere(":name IN (" + ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")")
                        .addWhereColumnName(colId)
                        .addWhereParameters(ids.toArray())
                )
                .forEach(statements::add);
    }

    /**
     * Get duplicated records
     * <p>
     * If there is VERSION duplication, choose:
     * <p>
     * - If UPDATE_TIME is: different -> pick more recent
     * <p>
     * - If UPDATE_TIME is: equal -> pick some random
     */
    private String getOlderDuplicatedRecords(String tableName, String colId, String colVersion, String colUpdateTime) {
        return """
                SELECT m1.%s
                FROM %s m1
                WHERE EXISTS (
                    SELECT m2.%s
                    FROM %s m2
                    WHERE m2.%s = m1.%s
                    AND (
                        m2.%s > m1.%s
                        OR (m2.%s = m1.%s AND m2.%s > m1.%s)
                    )
                )
                """.formatted(
                colId,                  // SELECT m1.%s         => SELECT m1.ID
                tableName,              // FROM %s m1           => FROM MIGRATION_MODEL m1
                colId,                  // SELECT m2.%s         => SELECT m2.ID
                tableName,              // FROM %s              => FROM MIGRATION_MODEL m2
                colVersion, colVersion, // WHERE m2.%s = m1.%s  => WHERE m2.VERSION = m1.VERSION
                colUpdateTime, colUpdateTime, // m2.%s > m1.%s  => m2.UPDATE_TIME > m1.UPDATE_TIME
                // OR (m2.%s = m1.%s AND m2.%s > m1.%s)         => OR (m2.UPDATE_TIME = m1.UPDATE_TIME AND m2.ID > m1.ID)
                colUpdateTime, colUpdateTime, colId, colId);
    }

}
