/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * <p>Same events provider factory than <em>TestEventsListenerProviderFactory</em> but
 * the implementation saves realm name and clientId  from the session context as details in
 * the event. This way we can ensure that session context is correctly
 * propagated to the event listener.</p>
 *
 * @author rmartinc
 */
public class TestEventsListenerContextDetailsProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "event-queue-context-details";
    public static final String CONTEXT_REALM_DETAIL = "context.realmName";
    public static final String CONTEXT_CLIENT_DETAIL = "context.clientId";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new TestEventsListenerContextDetailsProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
