package org.keycloak.events.hooks;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class EventHookFeatureSupportTest {

    @Test
    public void shouldRegisterEventHooksListenerGlobally() {
        assertTrue(new EventHookEventListenerProviderFactory().isGlobal());
    }
}
