/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.workflow.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pins the event types that count as client activity. An authorization code is only issued at the end of a
 * login, so an integration test cannot exercise {@code CODE_TO_TOKEN} apart from {@code LOGIN}; this covers
 * each type on its own.
 */
public class ClientActivityWorkflowEventProviderTest {

    private final ClientActivityWorkflowEventProvider provider =
            new ClientActivityWorkflowEventProvider(null, null, ClientActivityWorkflowEventFactory.ID);

    private static Event eventOf(EventType type) {
        Event event = new Event();
        event.setType(type);
        event.setClientId("some-client");
        return event;
    }

    @Test
    public void countsSuccessfulUseOfTheClient() {
        assertTrue(provider.supports(eventOf(EventType.LOGIN)));
        assertTrue(provider.supports(eventOf(EventType.CODE_TO_TOKEN)));
        assertTrue(provider.supports(eventOf(EventType.REFRESH_TOKEN)));
    }

    @Test
    public void ignoresFailedAttempts() {
        // Error events carry the client too; counting them would let failed attempts keep a dormant client alive
        assertFalse(provider.supports(eventOf(EventType.LOGIN_ERROR)));
        assertFalse(provider.supports(eventOf(EventType.CODE_TO_TOKEN_ERROR)));
        assertFalse(provider.supports(eventOf(EventType.REFRESH_TOKEN_ERROR)));
    }

    @Test
    public void leavesClientCredentialsToClientAuthenticated() {
        assertFalse(provider.supports(eventOf(EventType.CLIENT_LOGIN)));
    }
}
