/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventMatchers;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test getting and filtering login-related events.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class LoginEventsTest {

    @InjectRealm(config = LoginEventsRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectRunOnServer
    RunOnServerClient runOnServerClient;

    @BeforeEach
    public void init() {
        managedRealm.admin().clearEvents();
    }

    private List<EventRepresentation> events() {
        return managedRealm.admin().getEvents();
    }

    private void badLogin() {
        oAuthClient.doLogin("bad", "user");
    }

    private void pause(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void eventAttributesTest() {
        badLogin();
        List<EventRepresentation> events = events();
        Assertions.assertEquals(1, events.size());
        EventRepresentation event = events.get(0);
        assertThat(event.getId(), EventMatchers.isUUID());
        Assertions.assertTrue(event.getTime() > 0);
        Assertions.assertNotNull(event.getIpAddress());
        Assertions.assertEquals("LOGIN_ERROR", event.getType());
        Assertions.assertEquals(managedRealm.getId(), event.getRealmId());
        Assertions.assertNull(event.getUserId()); // no user for bad login
        Assertions.assertNull(event.getSessionId()); // no session for bad login
        Assertions.assertEquals("user_not_found", event.getError());

        Map<String, String> details = event.getDetails();
        Assertions.assertEquals("openid-connect", details.get("auth_method"));
        Assertions.assertEquals("code", details.get("auth_type"));
        Assertions.assertNotNull(details.get("redirect_uri"));
        Assertions.assertNotNull(details.get("code_id"));
        Assertions.assertEquals("bad", details.get("username"));
    }

    @Test
    public void clearEventsTest() {
        Assertions.assertEquals(0, events().size());
        badLogin();
        badLogin();
        Assertions.assertEquals(2, events().size());
        managedRealm.admin().clearEvents();
        Assertions.assertEquals(0, events().size());
    }

    @Test
    public void loggingOfCertainTypeTest() {
        Assertions.assertEquals(0, events().size());
        managedRealm.updateWithCleanup(r -> r.enabledEventTypes("REVOKE_GRANT"));

        badLogin();
        Assertions.assertEquals(0, events().size());

        managedRealm.updateWithCleanup(r -> r.setEnabledEventTypes("LOGIN_ERROR"));

        badLogin();
        Assertions.assertEquals(1, events().size());
    }

    @Test
    public void filterTest() {
        badLogin();
        badLogin();
        Assertions.assertEquals(2, events().size());

        List<EventRepresentation> filteredEvents = managedRealm.admin().getEvents(List.of("REVOKE_GRANT"), null, null, null, null, null, null, null);
        Assertions.assertEquals(0, filteredEvents.size());

        filteredEvents = managedRealm.admin().getEvents(List.of("LOGIN_ERROR"), null, null, null, null, null, null, null);
        Assertions.assertEquals(2, filteredEvents.size());
    }

    @Test
    public void defaultMaxResults() {
        String realmId = managedRealm.getId();

        runOnServerClient.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
            Event event = new Event();
            event.setRealmId(realmId);
            event.setType(EventType.LOGIN);

            for (int i = 0; i < 110; i++) {
                provider.onEvent(event);
            }
        });

        Assertions.assertEquals(100, managedRealm.admin().getEvents(null, null, null, null, null, null, null, null).size());
        Assertions.assertEquals(105, managedRealm.admin().getEvents(null, null, null, null, null, null, 0, 105).size());
        Assertions.assertTrue(managedRealm.admin().getEvents(null, null, null, null, null, null, 0, 1000).size() >= 110);
    }

    @Test
    public void orderResultsTest() {
        String realmId = managedRealm.getId();

        runOnServerClient.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);

            Event firstEvent = new Event();
            firstEvent.setRealmId(realmId);
            firstEvent.setType(EventType.LOGIN);
            firstEvent.setTime(System.currentTimeMillis() - 1000);

            Event secondEvent = new Event();
            secondEvent.setRealmId(realmId);
            secondEvent.setType(EventType.LOGOUT);
            secondEvent.setTime(System.currentTimeMillis());

            provider.onEvent(firstEvent);
            provider.onEvent(secondEvent);
        });

        List<EventRepresentation> events = managedRealm.admin().getEvents(null, null, null, null, null, null, null, null, "desc");
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(EventType.LOGOUT.toString(), events.get(0).getType());
        Assertions.assertEquals(EventType.LOGIN.toString(), events.get(1).getType());

        events = managedRealm.admin().getEvents(null, null, null, null, null, null, null, null, "asc");
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(EventType.LOGOUT.toString(), events.get(1).getType());
        Assertions.assertEquals(EventType.LOGIN.toString(), events.get(0).getType());
    }


    @Test
    public void filterByEpochTimeStamp() {
        long currentTime = System.currentTimeMillis();
        String realmId = managedRealm.getId();

        runOnServerClient.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
            Event event = new Event();
            event.setType(EventType.LOGIN);
            event.setRealmId(realmId);

            event.setTime(currentTime - 2*24*3600*1000);
            provider.onEvent(event);
            event.setTime(currentTime - 1000);
            provider.onEvent(event);
            event.setTime(currentTime);
            provider.onEvent(event);
            event.setTime(currentTime + 1000);
            provider.onEvent(event);
            event.setTime(currentTime + 2*24*3600*1000);
            provider.onEvent(event);
        });

        List<EventRepresentation> events = managedRealm.admin().getEvents();
        Assertions.assertEquals(5, events.size());
        events = managedRealm.admin().getEvents(null, null, null, currentTime, currentTime, null, null, null, null);
        Assertions.assertEquals(1, events.size());
        events = managedRealm.admin().getEvents(null, null, null, currentTime - 1000, currentTime + 1000, null, null, null, null);
        Assertions.assertEquals(3, events.size());

        LocalDate dateFrom = Instant.ofEpochMilli(currentTime - 1000).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate dateTo = Instant.ofEpochMilli(currentTime + 1000).atZone(ZoneOffset.UTC).toLocalDate();
        events = managedRealm.admin().getEvents(null, null, null, dateFrom.toString(), dateTo.toString(), null, null, null, null);
        Assertions.assertEquals(3, events.size());
    }

    @Test
    public void testErrorEventsAreNotStoredWhenDisabled() {
        managedRealm.updateWithCleanup(r -> r.eventsEnabled(false));

        badLogin();
        Assertions.assertEquals(0, events().size());
    }


    /*
    Removed this test because it takes too long.  The default interval for
    event cleanup is 15 minutes (900 seconds).  I don't have time to figure out
    a way to set the cleanup thread to a lower interval for testing.
    @Test
    public void eventExpirationTest() {
        configRep.setEventsExpiration(1L); //  second
        saveConfig();
        badLogin();
        assertEquals(1, events().size());
        pause(900); // pause 900 seconds
        assertEquals(0, events().size());
    }**/

    private static class LoginEventsRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.eventsEnabled(true);
        }
    }
}
