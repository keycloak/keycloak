package org.keycloak.events.hooks;

import java.util.Map;

import org.junit.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventHookPayloadBuilderTest {

    @Test
    public void shouldBuildUserPayload() {
        Event event = new Event();
        event.setId("event-1");
        event.setRealmId("realm-1");
        event.setTime(1234L);
        event.setType(EventType.LOGIN);
        event.setClientId("account");
        event.setUserId("user-1");
        event.setSessionId("session-1");
        event.setIpAddress("127.0.0.1");
        event.setDetails(Map.of("foo", "bar"));

        Map<String, Object> payload = EventHookPayloadBuilder.buildUserEventPayload(event);

        assertEquals("USER", payload.get("sourceType"));
        assertEquals("event-1", payload.get("eventId"));
        assertEquals("LOGIN", payload.get("eventType"));
        assertEquals("account", payload.get("clientId"));
        assertEquals(Map.of("foo", "bar"), payload.get("details"));
    }

    @Test
    public void shouldBuildAdminPayloadWithOptionalRepresentation() {
        AdminEvent event = new AdminEvent();
        event.setId("admin-1");
        event.setRealmId("realm-1");
        event.setTime(5678L);
        event.setOperationType(OperationType.CREATE);
        event.setResourceTypeAsString("USER");
        event.setResourcePath("users/user-1");
        event.setRepresentation("{\"id\":\"user-1\"}");
        event.setDetails(Map.of("source", "admin"));

        AuthDetails auth = new AuthDetails();
        auth.setRealmId("realm-1");
        auth.setClientId("security-admin-console");
        auth.setUserId("admin-user");
        auth.setIpAddress("10.0.0.1");
        event.setAuthDetails(auth);

        Map<String, Object> payload = EventHookPayloadBuilder.buildAdminEventPayload(event, true);

        assertEquals("ADMIN", payload.get("sourceType"));
        assertEquals("CREATE", payload.get("operationType"));
        assertEquals("USER", payload.get("resourceType"));
        assertTrue(payload.containsKey("representation"));
        assertEquals("security-admin-console", ((Map<?, ?>) payload.get("auth")).get("clientId"));

        Map<String, Object> withoutRepresentation = EventHookPayloadBuilder.buildAdminEventPayload(event, false);
        assertFalse(withoutRepresentation.containsKey("representation"));
    }
}
