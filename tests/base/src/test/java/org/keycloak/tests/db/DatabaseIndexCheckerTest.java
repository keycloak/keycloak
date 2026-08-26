package org.keycloak.tests.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.storage.database.jpa.DatabaseIndexChecker;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.tests.suites.DatabaseTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@KeycloakIntegrationTest
@DatabaseTest
public class DatabaseIndexCheckerTest {

    private static final String INDEX_NAME = "IDX_USER_SESSION_EXPIRATION_CREATED";
    private static final String TABLE_NAME = "OFFLINE_USER_SESSION";
    private static final String TEMP_INDEX_NAME = "IDX_USS_EXPIRATION_CREATED_TMP";

    @TestOnServer
    public void testDetectsMissingIndexWithCorrectSql(KeycloakSession session) {
        var factory = (JpaConnectionProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(JpaConnectionProvider.class);
        var schema = factory.getSchema();
        var checker = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema);

        assertThat(checker.getMissingIndexesName(), is(empty()));

        renameIndex(factory, INDEX_NAME, TEMP_INDEX_NAME);

        try {
            var missingSql = checker.getMissingIndexesSql();
            assertThat(missingSql, hasKey(INDEX_NAME));

            String sql = missingSql.get(INDEX_NAME);
            String dbProduct = getDatabaseProduct(factory);

            if (dbProduct.contains("postgresql")) {
                assertThat(sql, containsString("INCLUDE"));
                assertThat(sql, not(containsString("OPTIMIZE_FOR_SEQUENTIAL_KEY")));
            } else if (dbProduct.contains("microsoft")) {
                assertThat(sql, containsString("INCLUDE"));
                assertThat(sql, containsString("OPTIMIZE_FOR_SEQUENTIAL_KEY"));
            } else {
                assertThat(sql, not(containsString("INCLUDE")));
            }
        } finally {
            renameIndex(factory, TEMP_INDEX_NAME, INDEX_NAME);
        }

        assertThat(checker.getMissingIndexesName(), is(empty()));
    }

    private static String getDatabaseProduct(JpaConnectionProviderFactory factory) {
        try (Connection connection = factory.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database product name", e);
        }
    }

    private static void renameIndex(JpaConnectionProviderFactory factory, String fromName, String toName) {
        try (Connection connection = factory.getConnection();
             Statement stmt = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProduct = metaData.getDatabaseProductName().toLowerCase();
            boolean storesLower = metaData.storesLowerCaseIdentifiers();

            String fromId = storesLower ? fromName.toLowerCase() : fromName;
            String toId = storesLower ? toName.toLowerCase() : toName;
            String tableId = storesLower ? TABLE_NAME.toLowerCase() : TABLE_NAME;

            if (dbProduct.contains("mysql") || dbProduct.contains("mariadb")) {
                stmt.executeUpdate(String.format("ALTER TABLE %s RENAME INDEX %s TO %s", tableId, fromId, toId));
            } else if (dbProduct.contains("microsoft")) {
                String schemaPrefix = connection.getSchema() != null ? connection.getSchema() + "." : "";
                stmt.executeUpdate(String.format("EXEC sp_rename '%s%s.%s', '%s', 'INDEX'", schemaPrefix, tableId, fromId, toId));
            } else {
                String schemaName = connection.getSchema();
                String qualifiedFromId = schemaName != null ? schemaName + "." + fromId : fromId;
                stmt.executeUpdate(String.format("ALTER INDEX %s RENAME TO %s", qualifiedFromId, toId));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rename index " + fromName + " to " + toName, e);
        }
    }
}
