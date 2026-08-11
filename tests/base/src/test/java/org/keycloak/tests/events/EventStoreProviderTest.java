/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.events;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.events.TestEventsListenerContextDetailsProviderFactory;
import org.keycloak.tests.suites.DatabaseTest;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class EventStoreProviderTest {

    @InjectRealm(config = EventStoreRealm.class)
    ManagedRealm realm1;

    @InjectRealm(ref = "realm2", config = EventStoreRealm.class)
    ManagedRealm realm2;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    EventRunOnServerHelper eventHelper;

    @BeforeEach
    public void setup() {
        eventHelper = new EventRunOnServerHelper(runOnServer);
    }

    @AfterEach
    public void after() {
        eventHelper.clearEvents(realm1.getId(), realm2.getId());
    }

    @Test
    @DatabaseTest
    public void save() {
        eventHelper.storeEvent(create(EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
    }

    @Test
    @DatabaseTest
     // This looks like some database issue, test should get events which are newer or equal to requested time, however it gets only newer events from remote server
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        eventHelper.storeEvent(create(EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(newest, EventType.REGISTER, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(newest, EventType.REGISTER, realm1.getId(), "clientId", "userId2", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(EventType.LOGIN, realm2.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(oldest, EventType.LOGIN, realm1.getId(), "clientId2", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(EventType.LOGIN, realm1.getId(), "clientId", "userId2", "127.0.0.1", "error"));

        Assertions.assertEquals(4, eventHelper.queryEvents(realm1.getId(), null, "clientId", null, null, null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(3, eventHelper.queryEvents(realm1.getId(), List.of(EventType.LOGIN), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm2.getId(), List.of(EventType.LOGIN), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryEvents(realm1.getId(), List.of(EventType.LOGIN, EventType.REGISTER), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm2.getId(), List.of(EventType.LOGIN, EventType.REGISTER), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(3, eventHelper.queryEvents(realm1.getId(), null, null, "userId", null, null, null, null, null).length);

        Assertions.assertEquals(1, eventHelper.queryEvents(realm1.getId(), List.of(EventType.REGISTER), null, "userId", null, null, null, null, null).length);

        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, 2).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, 4, null).length);

        Assertions.assertEquals(newest, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, 1)[0].getTime());
        Assertions.assertEquals(oldest, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, 4, 1)[0].getTime());

        eventHelper.clearEvents(realm1.getId(), realm2.getId());

        Assertions.assertEquals(0, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, null).length);

        String d04 = "2015-03-04";
        String d05 = "2015-03-05";
        String d06 = "2015-03-06";
        String d07 = "2015-03-07";

        String d01 = "2015-03-01";
        String d03 = "2015-03-03";
        String d08 = "2015-03-08";
        String d10 = "2015-03-10";

        Calendar date04 = this.createFromDate(d04);
        Calendar date05 = this.createFromDate(d05);
        Calendar date06 = this.createFromDate(d06);
        Calendar date07 = this.createFromDate(d07);

        eventHelper.storeEvent(create(date04, EventType.LOGIN, realm1.getId(), "clientId", "userId", "error"));
        eventHelper.storeEvent(create(date04, EventType.LOGIN, realm1.getId(), "clientId", "userId", "error"));
        eventHelper.storeEvent(create(date05, EventType.REGISTER, realm1.getId(), "clientId", "userId", "error"));
        eventHelper.storeEvent(create(date05, EventType.REGISTER, realm1.getId(), "clientId", "userId", "error"));
        eventHelper.storeEvent(create(date06, EventType.CODE_TO_TOKEN, realm1.getId(), "clientId", "userId2", "error"));
        eventHelper.storeEvent(create(date06, EventType.LOGOUT, realm1.getId(), "clientId", "userId2", "error"));
        eventHelper.storeEvent(create(date07, EventType.UPDATE_PROFILE, realm2.getId(), "clientId2", "userId2", "error"));
        eventHelper.storeEvent(create(date07, EventType.UPDATE_EMAIL, realm2.getId(), "clientId2", "userId2", "error"));

        Assertions.assertEquals(6, eventHelper.queryEvents(realm1.getId(), null, "clientId", null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm2.getId(), null, "clientId2", null, null, null, null, null, null).length);

        Assertions.assertEquals(6, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm2.getId(), null, null, null, null, null, null, null, null).length);

        Assertions.assertEquals(4, eventHelper.queryEvents(realm1.getId(), null, null, "userId", null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), null, null, "userId2", null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm2.getId(), null, null, "userId2", null, null, null, null, null).length);

        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), List.of(EventType.LOGIN), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), List.of(EventType.REGISTER), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(4, eventHelper.queryEvents(realm1.getId(), List.of(EventType.LOGIN, EventType.REGISTER), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm1.getId(), List.of(EventType.CODE_TO_TOKEN), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm1.getId(), List.of(EventType.LOGOUT), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm2.getId(), List.of(EventType.UPDATE_PROFILE), null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm2.getId(), List.of(EventType.UPDATE_EMAIL), null, null, null, null, null, null, null).length);

        Assertions.assertEquals(6, eventHelper.queryEvents(realm1.getId(), null, null, null, d04, null, null, null, null).length);
        Assertions.assertEquals(6, eventHelper.queryEvents(realm1.getId(), null, null, null, null, d07, null, null, null).length);

        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), null, null, null, d06, null, null, null, null).length);
        Assertions.assertEquals(4, eventHelper.queryEvents(realm1.getId(), null, null, null, null, d05, null, null, null).length);

        Assertions.assertEquals(0, eventHelper.queryEvents(realm2.getId(), null, null, null, d08, null, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryEvents(realm2.getId(), null, null, null, null, d03, null, null, null).length);

        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), null, null, null, d04, d04, null, null, null).length);
        Assertions.assertEquals(6, eventHelper.queryEvents(realm1.getId(), null, null, null, d04, d07, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm2.getId(), null, null, null, d04, d07, null, null, null).length);
        Assertions.assertEquals(4, eventHelper.queryEvents(realm1.getId(), null, null, null, d05, d07, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm2.getId(), null, null, null, d05, d07, null, null, null).length);
        Assertions.assertEquals(4, eventHelper.queryEvents(realm1.getId(), null, null, null, d04, d05, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), null, null, null, d06, d07, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm2.getId(), null, null, null, d06, d07, null, null, null).length);

        Assertions.assertEquals(0, eventHelper.queryEvents(realm1.getId(), null, null, null, d01, d03, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryEvents(realm2.getId(), null, null, null, d01, d03, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryEvents(realm1.getId(), null, null, null, d08, d10, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryEvents(realm2.getId(), null, null, null, d08, d10, null, null, null).length);
    }

    @Test
    public void testEventBuilder() {
        realm1.updateWithCleanup(r -> r.eventsListeners("event-queue-context-details"));

        String expectedRealmName = realm1.getName();

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();

            EventBuilder event = new EventBuilder(realm, session)
                    .event(EventType.LOGIN)
                    .session("session1")
                    .user("user1")
                    .client("client1");
            event.clone().error(Errors.USER_NOT_FOUND);

            event.clone().success();

            for (TestEventsListenerContextDetailsProviderFactory.Details details : TestEventsListenerContextDetailsProviderFactory.DETAILS) {
                Assertions.assertEquals(expectedRealmName, details.realmName());
                Assertions.assertEquals("client1", details.clientId());
            }
        });

        // the two events should be retrieved from the store provider
        EventRepresentation[] events = eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, null);
        Assertions.assertEquals(2, events.length);
        EventRepresentation event = Arrays.stream(events).filter(e -> EventType.LOGIN.toString().equals(e.getType())).findFirst().orElse(null);
        Assertions.assertNotNull(event, "No LOGIN event found");
        Assertions.assertEquals("user1", event.getUserId());
        Assertions.assertEquals("client1", event.getClientId());
        Assertions.assertEquals("session1", event.getSessionId());
        event = Arrays.stream(events).filter(e -> EventType.LOGIN_ERROR.toString().equals(e.getType())).findFirst().orElse(null);
        Assertions.assertNotNull(event, "No LOGIN_ERROR event found");
        Assertions.assertEquals("user1", event.getUserId());
        Assertions.assertEquals("client1", event.getClientId());
        Assertions.assertEquals("session1", event.getSessionId());
    }

    @Test
    public void clear() {
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis() - 20000, EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis(), EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis(), EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realm2.getId(), "clientId", "userId", "127.0.0.1", "error"));

        eventHelper.clearEvents(realm1.getId());

        Assertions.assertEquals(0, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm2.getId(), null, null, null, null, null, null, null, null).length);
    }

    @Test
    public void lengthExceedLimit(){
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realm1.getId(), StringUtils.repeat("clientId", 100), "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, StringUtils.repeat(realm1.getId(), 100), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realm1.getId(), "clientId", StringUtils.repeat("userId", 100), "127.0.0.1", "error"));

        EventRepresentation event = create(System.currentTimeMillis() - 30000, EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error");
        event.setSessionId(StringUtils.repeat("sessionId", 100));
        eventHelper.storeEvent(event);
    }

    @Test
    public void maxLengthWithNull(){
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realm1.getId(), null, null, "127.0.0.1", "error"));
    }

    @Test
    public void clearOld() {
        eventHelper.storeEvent(create(System.currentTimeMillis() - 300000, EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis() - 200000, EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis(), EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis(), EventType.LOGIN, realm1.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis() - 300000, EventType.LOGIN, realm2.getId(), "clientId", "userId", "127.0.0.1", "error"));
        eventHelper.storeEvent(create(System.currentTimeMillis(), EventType.LOGIN, realm2.getId(), "clientId", "userId", "127.0.0.1", "error"));

        // Set expiration of events for realm1.getId() .
        realm1.updateWithCleanup(r -> r.eventsExpiration(100));

        // The first 2 events from realm1.getId() will be deleted
        eventHelper.clearExpiredEvents();
        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryEvents(realm2.getId(), null, null, null, null, null, null, null, null).length);

        // Set expiration of events for realm2.getId() as well
        realm2.updateWithCleanup(r -> r.eventsExpiration(100));

        // The first event from realm2.getId() will be deleted now
        eventHelper.clearExpiredEvents();
        Assertions.assertEquals(2, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryEvents(realm2.getId(), null, null, null, null, null, null, null, null).length);

        // set time offset to the future. The remaining 2 events from realm1.getId() and 1 event from realm2.getId() should be expired now
        timeOffSet.set(150);
        eventHelper.clearExpiredEvents();
        Assertions.assertEquals(0, eventHelper.queryEvents(realm1.getId(), null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryEvents(realm2.getId(), null, null, null, null, null, null, null, null).length);
    }

    private EventRepresentation create(EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        return create(System.currentTimeMillis(), event, realmId, clientId, userId, ipAddress, error);
    }

    private EventRepresentation create(Calendar date, EventType event, String realmId, String clientId, String userId, String error) {
        return create(date.getTimeInMillis(), event, realmId, clientId, userId, "127.0.0.1", error);
    }

    private EventRepresentation create(long time, EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        EventRepresentation e = new EventRepresentation();
        e.setTime(time);
        e.setType(event.toString());
        e.setRealmId(realmId);
        e.setClientId(clientId);
        e.setUserId(userId);
        e.setIpAddress(ipAddress);
        e.setError(error);

        Map<String, String> details = new HashMap<String, String>();
        details.put("key1", "value1");
        details.put("key2", "value2");

        e.setDetails(details);

        return e;
    }

    /**
     * Creates a {@link Calendar} from the specified date string, which must be in the {@code yyyy-MM-dd} format. Once
     * the date is parsed, this method creates a {@link Calendar} instance and sets a random time within that date.
     *
     * @param dateString a string representing a date in the format {@code yyyy-MM-dd}
     * @return the {@link Calendar} representing the date with a random time set to it, or {@code null} if the specified
     * date string is not in the expected format.
     */
    private Calendar createFromDate(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Random random = new Random();
        Calendar result = null;
        try {
            Date date = formatter.parse(dateString);
            result = new GregorianCalendar();
            result.setTime(date);
            result.set(Calendar.HOUR_OF_DAY, random.nextInt(0, 24));
            result.set(Calendar.MINUTE, random.nextInt(0, 60));
            result.set(Calendar.SECOND, random.nextInt(0, 60));
            result.set(Calendar.MILLISECOND, random.nextInt(0, 1000));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static final class EventStoreRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.eventsEnabled(true).eventsExpiration(0);
        }
    }

}
