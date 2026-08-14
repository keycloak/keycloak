package org.keycloak.events.hooks;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqlEventHookTargetProviderTest {

    @Test
    public void shouldDeliverPreparedStatementUsingPayloadValues() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:event_hook_target;DB_CLOSE_DELAY=-1";
        createSchema(jdbcUrl);

        SqlEventHookTargetProvider provider = new SqlEventHookTargetProvider();
        EventHookDeliveryResult result = provider.deliver(target(jdbcUrl), message(Map.of(
                "eventId", "evt-1",
                "eventType", "LOGIN",
                "details", Map.of("clientId", "account-console")
        )));

        assertTrue(result.isSuccess());
        assertFalse(result.isRetryable());
        assertEquals("SQL_OK", result.getStatusCode());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select EVENT_ID, EVENT_TYPE, PAYLOAD_JSON from EVENT_HOOK_AUDIT")) {
            assertTrue(resultSet.next());
            assertEquals("evt-1", resultSet.getString(1));
            assertEquals("LOGIN", resultSet.getString(2));
            String payloadJson = resultSet.getString(3);
            assertTrue(payloadJson.contains("evt-1"));
            assertEquals(false, resultSet.next());
        }
    }

    @Test
    public void shouldDeliverPreparedStatementUsingNestedPayloadProperty() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:event_hook_target_nested;DB_CLOSE_DELAY=-1";
        createSchema(jdbcUrl);

        SqlEventHookTargetProvider provider = new SqlEventHookTargetProvider();
        EventHookDeliveryResult result = provider.deliver(nestedTarget(jdbcUrl), message(Map.of(
                "eventId", "evt-2",
                "eventType", "REGISTER",
                "details", Map.of(
                        "clientId", "security-admin-console",
                        "metadata", Map.of("source", "admin-ui"))
        )));

        assertTrue(result.isSuccess());
        assertFalse(result.isRetryable());
        assertEquals("SQL_OK", result.getStatusCode());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select EVENT_ID, EVENT_TYPE, CLIENT_ID, SOURCE from EVENT_HOOK_AUDIT")) {
            assertTrue(resultSet.next());
            assertEquals("evt-2", resultSet.getString(1));
            assertEquals("REGISTER", resultSet.getString(2));
            assertEquals("security-admin-console", resultSet.getString(3));
            assertEquals("admin-ui", resultSet.getString(4));
            assertEquals(false, resultSet.next());
        }
    }

    @Test
    public void shouldDeliverPreparedStatementUsingNestedPayloadPathPrefixedWithFullPayload() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:event_hook_target_nested_prefixed;DB_CLOSE_DELAY=-1";
        createSchema(jdbcUrl);

        SqlEventHookTargetProvider provider = new SqlEventHookTargetProvider();
        EventHookDeliveryResult result = provider.deliver(prefixedNestedTarget(jdbcUrl), message(Map.of(
                "eventId", "evt-3",
                "eventType", "UPDATE",
                "details", Map.of(
                        "metadata", Map.of("source", "prefixed"))
        )));

        assertTrue(result.isSuccess());
        assertFalse(result.isRetryable());
        assertEquals("SQL_OK", result.getStatusCode());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select EVENT_ID, EVENT_TYPE, SOURCE from EVENT_HOOK_AUDIT")) {
            assertTrue(resultSet.next());
            assertEquals("evt-3", resultSet.getString(1));
            assertEquals("UPDATE", resultSet.getString(2));
            assertEquals("prefixed", resultSet.getString(3));
            assertEquals(false, resultSet.next());
        }
    }

    @Test
    public void shouldBindNullWhenNestedPayloadPathIsMissing() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:event_hook_target_missing_nested;DB_CLOSE_DELAY=-1";
        createSchema(jdbcUrl);

        SqlEventHookTargetProvider provider = new SqlEventHookTargetProvider();
        EventHookDeliveryResult result = provider.deliver(missingNestedTarget(jdbcUrl), message(Map.of(
                "eventId", "evt-4",
                "eventType", "DELETE",
                "details", Map.of(
                        "clientId", "account-console",
                        "roles", List.of("admin"))
        )));

        assertTrue(result.isSuccess());
        assertFalse(result.isRetryable());
        assertEquals("SQL_OK", result.getStatusCode());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select EVENT_ID, EVENT_TYPE, CLIENT_ID, SOURCE from EVENT_HOOK_AUDIT")) {
            assertTrue(resultSet.next());
            assertEquals("evt-4", resultSet.getString(1));
            assertEquals("DELETE", resultSet.getString(2));
            assertEquals("account-console", resultSet.getString(3));
            assertEquals(null, resultSet.getString(4));
            assertEquals(false, resultSet.next());
        }
    }

    @Test
    public void shouldResolveNestedRepresentationPropertyWhenRepresentationContainsJson() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:event_hook_target_representation;DB_CLOSE_DELAY=-1";
        createSchema(jdbcUrl);

        SqlEventHookTargetProvider provider = new SqlEventHookTargetProvider();
        EventHookDeliveryResult result = provider.deliver(representationTarget(jdbcUrl), message(Map.of(
                "eventId", "evt-5",
                "eventType", "UPDATE",
                "representation", "{\"id\":\"user-7\",\"enabled\":true}"
        )));

        assertTrue(result.isSuccess());
        assertFalse(result.isRetryable());
        assertEquals("SQL_OK", result.getStatusCode());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select EVENT_ID, EVENT_TYPE, CLIENT_ID from EVENT_HOOK_AUDIT")) {
            assertTrue(resultSet.next());
            assertEquals("evt-5", resultSet.getString(1));
            assertEquals("UPDATE", resultSet.getString(2));
            assertEquals("user-7", resultSet.getString(3));
            assertEquals(false, resultSet.next());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBatchDelivery() {
        new SqlEventHookTargetProvider().deliverBatch(target("jdbc:h2:mem:event_hook_batch;DB_CLOSE_DELAY=-1"), List.of());
    }

    private void createSchema(String jdbcUrl) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("create table EVENT_HOOK_AUDIT (EVENT_ID varchar(255), EVENT_TYPE varchar(255), CLIENT_ID varchar(255), SOURCE varchar(255), PAYLOAD_JSON clob)");
        }
    }

    private EventHookTargetModel target(String jdbcUrl) {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(Map.of(
                "databaseKind", "h2",
                "jdbcUrl", jdbcUrl,
                "jdbcUsername", "sa",
                "jdbcPassword", "",
            "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, PAYLOAD_JSON) values (:eventId, :eventType, :payload)",
                "queryTimeoutSeconds", 10
        ));
        return target;
    }

    private EventHookTargetModel nestedTarget(String jdbcUrl) {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(Map.of(
                "databaseKind", "h2",
                "jdbcUrl", jdbcUrl,
                "jdbcUsername", "sa",
                "jdbcPassword", "",
            "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, CLIENT_ID, SOURCE) values (:eventId, :eventType, :details.clientId, :details.metadata.source)",
                "queryTimeoutSeconds", 10
        ));
        return target;
    }

    private EventHookTargetModel prefixedNestedTarget(String jdbcUrl) {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(Map.of(
                "databaseKind", "h2",
                "jdbcUrl", jdbcUrl,
                "jdbcUsername", "sa",
                "jdbcPassword", "",
            "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, SOURCE) values (:eventId, :eventType, :payload.details.metadata.source)",
                "queryTimeoutSeconds", 10
        ));
        return target;
    }

    private EventHookTargetModel missingNestedTarget(String jdbcUrl) {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(Map.of(
                "databaseKind", "h2",
                "jdbcUrl", jdbcUrl,
                "jdbcUsername", "sa",
                "jdbcPassword", "",
            "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, CLIENT_ID, SOURCE) values (:eventId, :eventType, :payload.details.clientId, :payload.details.roles.missing)",
                "queryTimeoutSeconds", 10
        ));
        return target;
    }

    private EventHookTargetModel representationTarget(String jdbcUrl) {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(Map.of(
                "databaseKind", "h2",
                "jdbcUrl", jdbcUrl,
                "jdbcUsername", "sa",
                "jdbcPassword", "",
            "sqlStatement", "insert into EVENT_HOOK_AUDIT (EVENT_ID, EVENT_TYPE, CLIENT_ID) values (:eventId, :eventType, :payload.representation.id)",
                "queryTimeoutSeconds", 10
        ));
        return target;
    }

    private EventHookMessageModel message(Map<String, Object> payload) throws IOException {
        EventHookMessageModel message = new EventHookMessageModel();
        message.setId("msg-1");
        message.setPayload(JsonSerialization.writeValueAsString(payload));
        return message;
    }
}
