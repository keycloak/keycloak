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
import static org.hamcrest.Matchers.hasItem;
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
        var checker = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema, false, 0);

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

    @TestOnServer
    public void testDetectsAndRecreatesInvalidIndexOnPostgresql(KeycloakSession session) {
        var factory = (JpaConnectionProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(JpaConnectionProvider.class);

        String dbProduct = getDatabaseProduct(factory);
        if (!dbProduct.contains("postgresql")) {
            return;
        }

        var schema = factory.getSchema();

        createInvalidIndex(factory, INDEX_NAME, schema);
        boolean recreated = false;

        try {
            var detectOnly = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema, false, 0);
            assertThat(detectOnly.getMissingIndexesName(), hasItem(INDEX_NAME));

            var autoCreate = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema, true, 0);
            autoCreate.run();

            var afterRun = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema, false, 0);
            assertThat(afterRun.getMissingIndexesName(), is(empty()));
            recreated = true;
        } finally {
            if (!recreated) {
                dropIndex(factory, INDEX_NAME);
                recreateIndex(factory, session, schema);
            }
        }
    }

    @TestOnServer
    public void testAutoCreatesIndexWhenSupported(KeycloakSession session) {
        var factory = (JpaConnectionProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(JpaConnectionProvider.class);
        var schema = factory.getSchema();
        boolean onlineSupported = supportsOnlineIndexCreation(factory);

        // Drop instead of rename — Oracle rejects creating a second index on the same column list (ORA-01408)
        dropIndex(factory, INDEX_NAME);
        boolean recreated = false;

        try {
            var detectOnly = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema, false, 0);
            assertThat(detectOnly.getMissingIndexesName(), not(empty()));

            var autoCreate = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema, true, 0);
            autoCreate.run();

            var afterRun = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema, false, 0);
            if (onlineSupported) {
                assertThat(afterRun.getMissingIndexesName(), is(empty()));
                recreated = true;
            } else {
                assertThat(afterRun.getMissingIndexesName(), not(empty()));
            }
        } finally {
            if (!recreated) {
                recreateIndex(factory, session, schema);
            }
        }
    }

    private static boolean supportsOnlineIndexCreation(JpaConnectionProviderFactory factory) {
        try (Connection connection = factory.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();
            if (productName.contains("postgres")
                    || productName.contains("mysql") || productName.contains("mariadb")) {
                return true;
            }
            if (productName.contains("oracle")) {
                try (Statement stmt = connection.createStatement();
                     var rs = stmt.executeQuery("SELECT BANNER FROM V$VERSION WHERE BANNER LIKE 'Oracle%' AND ROWNUM <= 1")) {
                    if (rs.next()) {
                        String banner = rs.getString(1);
                        return banner != null && banner.toUpperCase().contains("ENTERPRISE");
                    }
                }
                return false;
            }
            if (productName.contains("microsoft")) {
                try (Statement stmt = connection.createStatement();
                     var rs = stmt.executeQuery("SELECT CAST(SERVERPROPERTY('EngineEdition') AS INT)")) {
                    if (rs.next()) {
                        int edition = rs.getInt(1);
                        return edition == 3 || edition == 5 || edition == 8;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    private static String getDatabaseProduct(JpaConnectionProviderFactory factory) {
        try (Connection connection = factory.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database product name", e);
        }
    }

    private static void recreateIndex(JpaConnectionProviderFactory factory, KeycloakSession session, String schema) {
        var checker = new DatabaseIndexChecker(factory::getConnection, session.getKeycloakSessionFactory(), schema, false, 0);
        String sql = checker.getMissingIndexesSql().get(INDEX_NAME);
        if (sql == null) {
            return;
        }
        try (Connection connection = factory.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to recreate index " + INDEX_NAME, e);
        }
    }

    private static void dropIndex(JpaConnectionProviderFactory factory, String indexName) {
        try (Connection connection = factory.getConnection();
             Statement stmt = connection.createStatement()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProduct = metaData.getDatabaseProductName().toLowerCase();
            boolean storesLower = metaData.storesLowerCaseIdentifiers();

            String indexId = storesLower ? indexName.toLowerCase() : indexName;
            String tableId = storesLower ? TABLE_NAME.toLowerCase() : TABLE_NAME;

            if (dbProduct.contains("mysql") || dbProduct.contains("mariadb")) {
                stmt.executeUpdate(String.format("ALTER TABLE %s DROP INDEX %s", tableId, indexId));
            } else if (dbProduct.contains("microsoft")) {
                String schemaPrefix = connection.getSchema() != null ? connection.getSchema() + "." : "";
                stmt.executeUpdate(String.format("DROP INDEX %s ON %s%s", indexId, schemaPrefix, tableId));
            } else if (dbProduct.contains("oracle")) {
                stmt.executeUpdate(String.format("DROP INDEX %s", indexId));
            } else {
                String schemaName = connection.getSchema();
                String qualifiedIndexId = schemaName != null ? schemaName + "." + indexId : indexId;
                stmt.executeUpdate(String.format("DROP INDEX IF EXISTS %s", qualifiedIndexId));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to drop index " + indexName, e);
        }
    }

    private static void createInvalidIndex(JpaConnectionProviderFactory factory, String indexName, String schema) {
        String indexId = indexName.toLowerCase();
        String tableId = TABLE_NAME.toLowerCase();
        String qualifiedTable = schema != null ? schema + "." + tableId : tableId;
        String qualifiedIndex = schema != null ? schema + "." + indexId : indexId;

        String columns = "user_session_id, offline_flag, user_id, realm_id, created_on, last_session_refresh, version, session_bucket, last_session_refresh_coarse";
        String values = "'%s', '0', '__test', '__test', 0, 0, 0, 0, 0";

        dropIndex(factory, indexName);

        try (Connection connection = factory.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(String.format("INSERT INTO %s (%s) VALUES (%s)", qualifiedTable, columns, String.format(values, "__test_invalid_idx_1")));
            stmt.executeUpdate(String.format("INSERT INTO %s (%s) VALUES (%s)", qualifiedTable, columns, String.format(values, "__test_invalid_idx_2")));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert test rows", e);
        }

        try (Connection connection = factory.getConnection()) {
            connection.setAutoCommit(true);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(String.format("CREATE UNIQUE INDEX CONCURRENTLY %s ON %s (realm_id)", qualifiedIndex, qualifiedTable));
            }
        } catch (SQLException e) {
            // Expected: unique violation leaves an invalid index
        }

        try (Connection connection = factory.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(String.format("DELETE FROM %s WHERE user_session_id LIKE '__test_invalid_idx_%%'", qualifiedTable));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clean up test rows", e);
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
