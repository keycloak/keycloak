package org.keycloak.events.hooks;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqlEventHookTargetProviderFactoryTest {

    private final SqlEventHookTargetProviderFactory factory = new SqlEventHookTargetProviderFactory();

        @Test
        public void shouldExposeAllSqlSettings() {
        assertEquals(
            List.of(
                "databaseKind",
                "jdbcUrl",
                "jdbcUsername",
                "jdbcPassword",
                "sqlStatement",
                "sqlParameters",
                "queryTimeoutSeconds"
            ),
            factory.getConfigMetadata().stream().map(ProviderConfigProperty::getName).toList()
        );
        }

    @Test
    public void shouldRejectBatchSupport() {
        assertFalse(factory.supportsBatch());
    }

    @Test
    public void shouldAcceptValidSqlSettings() {
        factory.validateConfig(null, Map.of(
                "databaseKind", "h2",
                "jdbcUrl", "jdbc:h2:mem:eventhook;DB_CLOSE_DELAY=-1",
                "jdbcUsername", "sa",
                "jdbcPassword", "secret",
                "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, PAYLOAD_JSON) values (?, ?, ?)",
                "sqlParameters", List.of("eventId", "eventType", "$payload"),
                "queryTimeoutSeconds", 10
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectPlaceholderMismatch() {
        factory.validateConfig(null, Map.of(
                "databaseKind", "h2",
                "jdbcUrl", "jdbc:h2:mem:eventhook;DB_CLOSE_DELAY=-1",
                "jdbcUsername", "sa",
                "jdbcPassword", "secret",
                "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE) values (?, ?)",
                "sqlParameters", List.of("eventId")
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidJdbcUrl() {
        factory.validateConfig(null, Map.of(
                "databaseKind", "h2",
                "jdbcUrl", "http://example.org/not-jdbc",
                "jdbcUsername", "sa",
                "jdbcPassword", "secret",
                "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID) values (?)",
                "sqlParameters", List.of("eventId")
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingJdbcUsername() {
        factory.validateConfig(null, Map.of(
                "databaseKind", "h2",
                "jdbcUrl", "jdbc:h2:mem:eventhook;DB_CLOSE_DELAY=-1",
                "jdbcPassword", "secret",
                "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID) values (?)",
                "sqlParameters", List.of("eventId")
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveQueryTimeout() {
        factory.validateConfig(null, Map.of(
                "databaseKind", "h2",
                "jdbcUrl", "jdbc:h2:mem:eventhook;DB_CLOSE_DELAY=-1",
                "jdbcUsername", "sa",
                "jdbcPassword", "secret",
                "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID) values (?)",
                "sqlParameters", List.of("eventId"),
                "queryTimeoutSeconds", 0
        ));
    }

    @Test
    public void shouldExposeDisplayInfoFromDatabaseKind() {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(Map.of("databaseKind", "postgres"));

        assertEquals("POSTGRES: prepared statement", factory.getDisplayInfo(target));
    }

    @Test
    public void shouldRedactConfidentialConnectionSettings() {
        Map<String, Object> redacted = factory.redactConfig(Map.of(
                "databaseKind", "postgres",
                "jdbcUrl", "jdbc:postgresql://db.example/keycloak",
                "jdbcUsername", "db-user",
                "jdbcPassword", "db-password",
                "sqlStatement", "insert into event_hook_audit(payload_json) values (?)"
        ));

        assertEquals(EventHookTargetProviderFactory.REDACTED_SECRET_VALUE, redacted.get("jdbcUrl"));
        assertEquals(EventHookTargetProviderFactory.REDACTED_SECRET_VALUE, redacted.get("jdbcUsername"));
        assertEquals(EventHookTargetProviderFactory.REDACTED_SECRET_VALUE, redacted.get("jdbcPassword"));
    }

    @Test
    public void shouldExecuteTestDeliveryAgainstSqlTarget() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:eventhook_factory_test;DB_CLOSE_DELAY=-1";
        createSchema(jdbcUrl);

        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-1");
        target.setType(SqlEventHookTargetProviderFactory.ID);
        target.setName("SQL target");
        target.setEnabled(true);
        target.setSettings(Map.of(
                "databaseKind", "h2",
                "jdbcUrl", jdbcUrl,
                "jdbcUsername", "sa",
                "jdbcPassword", "",
                "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, PAYLOAD_JSON) values (?, ?, ?)",
                "sqlParameters", List.of("messageId", "eventType", "$payload"),
                "queryTimeoutSeconds", 10
        ));

        EventHookDeliveryResult result = factory.test(null, realm("realm-1", "demo"), target);

        assertTrue(result.isSuccess());
        assertEquals("SQL_OK", result.getStatusCode());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select EVENT_ID, EVENT_TYPE from EVENT_HOOK_AUDIT")) {
            assertTrue(resultSet.next());
            assertTrue(resultSet.getString(1).startsWith("test-"));
            assertEquals("EVENT_HOOK_TEST", resultSet.getString(2));
            assertFalse(resultSet.next());
        }
    }

    @Test
    public void shouldExecuteTestDeliveryUsingNestedTestPayloadProperty() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:eventhook_factory_nested_test;DB_CLOSE_DELAY=-1";
        createSchema(jdbcUrl);

        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-2");
        target.setType(SqlEventHookTargetProviderFactory.ID);
        target.setName("Nested SQL target");
        target.setEnabled(true);
        target.setSettings(Map.of(
                "databaseKind", "h2",
                "jdbcUrl", jdbcUrl,
                "jdbcUsername", "sa",
                "jdbcPassword", "",
                "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, REALM_NAME, TARGET_TYPE) values (?, ?, ?, ?)",
                "sqlParameters", List.of("messageId", "eventType", "realm.name", "target.type"),
                "queryTimeoutSeconds", 10
        ));

        EventHookDeliveryResult result = factory.test(null, realm("realm-1", "demo"), target);

        assertTrue(result.isSuccess());
        assertEquals("SQL_OK", result.getStatusCode());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select EVENT_ID, EVENT_TYPE, REALM_NAME, TARGET_TYPE from EVENT_HOOK_AUDIT")) {
            assertTrue(resultSet.next());
            assertTrue(resultSet.getString(1).startsWith("test-"));
            assertEquals("EVENT_HOOK_TEST", resultSet.getString(2));
            assertEquals("demo", resultSet.getString(3));
            assertEquals("sql", resultSet.getString(4));
            assertFalse(resultSet.next());
        }
    }

    @Test
    public void shouldExecuteTestDeliveryUsingNestedFullPayloadPath() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:eventhook_factory_prefixed_nested_test;DB_CLOSE_DELAY=-1";
        createSchema(jdbcUrl);

        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-3");
        target.setType(SqlEventHookTargetProviderFactory.ID);
        target.setName("Prefixed nested SQL target");
        target.setEnabled(true);
        target.setSettings(Map.of(
                "databaseKind", "h2",
                "jdbcUrl", jdbcUrl,
                "jdbcUsername", "sa",
                "jdbcPassword", "",
                "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, REALM_NAME, TARGET_TYPE) values (?, ?, ?, ?)",
                "sqlParameters", List.of("messageId", "eventType", "$payload.realm.name", "$payload.target.type"),
                "queryTimeoutSeconds", 10
        ));

        EventHookDeliveryResult result = factory.test(null, realm("realm-1", "demo"), target);

        assertTrue(result.isSuccess());
        assertEquals("SQL_OK", result.getStatusCode());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select EVENT_ID, EVENT_TYPE, REALM_NAME, TARGET_TYPE from EVENT_HOOK_AUDIT")) {
            assertTrue(resultSet.next());
            assertTrue(resultSet.getString(1).startsWith("test-"));
            assertEquals("EVENT_HOOK_TEST", resultSet.getString(2));
            assertEquals("demo", resultSet.getString(3));
            assertEquals("sql", resultSet.getString(4));
            assertFalse(resultSet.next());
        }
    }

    private RealmModel realm(String realmId, String realmName) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> realmId;
                    case "getName" -> realmName;
                    default -> null;
                });
    }

    private void createSchema(String jdbcUrl) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("create table EVENT_HOOK_AUDIT (EVENT_ID varchar(255), EVENT_TYPE varchar(255), REALM_NAME varchar(255), TARGET_TYPE varchar(255), PAYLOAD_JSON clob)");
        }
    }
}
