package org.keycloak.events.hooks;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventHookTargetEventFilterTest {

    @Test
    public void shouldMatchAllEventsWhenFilterIsMissing() {
        assertTrue(EventHookTargetEventFilter.matchesUserEvent(target(Map.of()), userEvent(EventType.LOGIN)));
        assertTrue(EventHookTargetEventFilter.matchesAdminEvent(target(Map.of()), adminEvent(OperationType.CREATE)));
    }

    @Test
    public void shouldMatchConfiguredUserEventNames() {
        EventHookTargetModel target = target(Map.of("events", List.of("LOGIN", "REGISTER")));

        assertTrue(EventHookTargetEventFilter.matchesUserEvent(target, userEvent(EventType.LOGIN)));
        assertFalse(EventHookTargetEventFilter.matchesUserEvent(target, userEvent(EventType.LOGOUT)));
    }

    @Test
    public void shouldMatchConfiguredAdminOperationNames() {
        EventHookTargetModel target = target(Map.of("events", List.of("CREATE")));

        assertTrue(EventHookTargetEventFilter.matchesAdminEvent(target, adminEvent(OperationType.CREATE)));
        assertFalse(EventHookTargetEventFilter.matchesAdminEvent(target, adminEvent(OperationType.DELETE)));
    }

    @Test
    public void shouldSupportScopedFiltersAndWildcard() {
        EventHookTargetModel target = target(Map.of("events", "USER:LOGIN##ADMIN:*"));

        assertTrue(EventHookTargetEventFilter.matchesUserEvent(target, userEvent(EventType.LOGIN)));
        assertFalse(EventHookTargetEventFilter.matchesUserEvent(target, userEvent(EventType.REGISTER)));
        assertTrue(EventHookTargetEventFilter.matchesAdminEvent(target, adminEvent(OperationType.ACTION)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnknownFilterValues() {
        EventHookTargetEventFilter.validateSettings(List.of("NOT_A_REAL_EVENT"));
    }

    private EventHookTargetModel target(Map<String, Object> settings) {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(settings);
        return target;
    }

    private Event userEvent(EventType eventType) {
        Event event = new Event();
        event.setType(eventType);
        return event;
    }

    private AdminEvent adminEvent(OperationType operationType) {
        AdminEvent event = new AdminEvent();
        event.setOperationType(operationType);
        return event;
    }
}
