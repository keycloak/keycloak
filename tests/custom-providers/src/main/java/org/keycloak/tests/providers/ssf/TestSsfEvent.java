package org.keycloak.tests.providers.ssf;

import org.keycloak.ssf.event.SsfEvent;

/**
 * Custom {@link SsfEvent} used by the SSF Transmitter custom-event
 * integration tests. Carries a distinctive event type URI and alias so
 * tests can assert the event was round-tripped through the registry.
 */
public class TestSsfEvent extends SsfEvent {

    public static final String TYPE = "https://tests.keycloak.org/secevent/test/event-type/custom";

    public TestSsfEvent() {
        super(TYPE);
    }
}
