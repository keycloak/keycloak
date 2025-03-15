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
package org.keycloak.tests.admin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.datastore.PeriodicEventInvalidation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

@KeycloakIntegrationTest
public class TimeOffsetTest {

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectRealm
    ManagedRealm managedRealm;

    @Test
    public void testOffset() {
        String realmId = managedRealm.getId();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            realm.setEventsExpiration(5);
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);

            Event e = new Event();
            e.setType(EventType.LOGIN);
            e.setTime(Time.currentTimeMillis());
            e.setRealmId(realmId);
            provider.onEvent(e);
        });

        runOnServer.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
            Assertions.assertEquals(1, provider.createQuery().realm(realmId).getResultStream().count());
        });

        timeOffSet.set(5);
        runOnServer.run(session -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.clearExpiredEvents();
            session.invalidate(PeriodicEventInvalidation.JPA_EVENT_STORE);
        });

        runOnServer.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
            Assertions.assertEquals(0, provider.createQuery().realm(realmId).getResultStream().count());
        });
    }
}
