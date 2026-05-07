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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
@DatabaseTest
public class DatabaseIndexCheckerTest {

    private static final String INDEX_NAME = "IDX_REALM_MASTER_ADM_CLI";
    private static final String TABLE_NAME = "REALM";
    private static final String COLUMN_NAME = "MASTER_ADMIN_CLIENT";

    @TestOnServer
    public void testDetectsMissingIndex(KeycloakSession session) {
        var factory = (JpaConnectionProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(JpaConnectionProvider.class);
        var schema = factory.getSchema();
        var checker = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema);

        assertThat(checker.getMissingIndexesName(), is(empty()));

        dropIndex(factory);

        try {
            var missing = checker.getMissingIndexesName();
            assertThat(missing, hasItem(INDEX_NAME));
            assertThat(missing.size(), is(1));
        } finally {
            createIndex(factory);
        }

        assertThat(checker.getMissingIndexesName(), is(empty()));
    }

    private static void dropIndex(JpaConnectionProviderFactory factory) {
        try (Connection connection = factory.getConnection();
             Statement stmt = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProduct = metaData.getDatabaseProductName().toLowerCase();
            String indexId = metaData.storesLowerCaseIdentifiers() ? INDEX_NAME.toLowerCase() : INDEX_NAME;
            String tableId = metaData.storesLowerCaseIdentifiers() ? TABLE_NAME.toLowerCase() : TABLE_NAME;

            if (dbProduct.contains("mysql") || dbProduct.contains("mariadb")) {
                stmt.executeUpdate(String.format("DROP INDEX %s ON %s", indexId, tableId));
            } else if (dbProduct.contains("microsoft")) {
                String schemaPrefix = connection.getSchema() != null ? connection.getSchema() + "." : "";
                stmt.executeUpdate(String.format("DROP INDEX %s ON %s%s", indexId, schemaPrefix, tableId));
            } else {
                stmt.executeUpdate(String.format("DROP INDEX %s", indexId));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to drop index " + INDEX_NAME, e);
        }
    }

    private static void createIndex(JpaConnectionProviderFactory factory) {
        try (Connection connection = factory.getConnection();
             Statement stmt = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String indexId = metaData.storesLowerCaseIdentifiers() ? INDEX_NAME.toLowerCase() : INDEX_NAME;
            String tableId = metaData.storesLowerCaseIdentifiers() ? TABLE_NAME.toLowerCase() : TABLE_NAME;
            String columnId = metaData.storesLowerCaseIdentifiers() ? COLUMN_NAME.toLowerCase() : COLUMN_NAME;

            stmt.executeUpdate(String.format("CREATE INDEX %s ON %s (%s)", indexId, tableId, columnId));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to recreate index " + INDEX_NAME, e);
        }
    }
}
