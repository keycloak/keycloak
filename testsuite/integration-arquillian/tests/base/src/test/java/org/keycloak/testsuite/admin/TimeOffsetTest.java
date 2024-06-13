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
package org.keycloak.testsuite.admin;

import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeOffsetTest extends AbstractAdminTest {

    @Test
    public void testOffset() {
        String realmId = adminClient.realm(REALM_NAME).toRepresentation().getId();
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);
            realm.setEventsExpiration(5);
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);

            Event e = new Event();
            e.setType(EventType.LOGIN);
            e.setTime(Time.currentTimeMillis());
            e.setRealmId(realmId);
            provider.onEvent(e);
        });

        testingClient.server().run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
            assertEquals(1, provider.createQuery().realm(realmId).getResultStream().count());
        });

        setTimeOffset(5);

        testingClient.testing().clearExpiredEvents();

        testingClient.server().run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
            assertEquals(0, provider.createQuery().realm(realmId).getResultStream().count());
        });
    }
}
