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

package org.keycloak.testsuite.events;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class EventStoreProviderTest extends AbstractEventsTest {

    @Rule
    public AssertEvents assertEvents = new AssertEvents(this);

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        for (String realmId : new String[] {REALM_NAME_1, REALM_NAME_2}) {
            RealmRepresentation adminRealmRep = new RealmRepresentation();
            adminRealmRep.setId(realmId);
            adminRealmRep.setRealm(realmId);
            adminRealmRep.setEnabled(true);
            adminRealmRep.setEventsEnabled(true);
            adminRealmRep.setEventsExpiration(0);
            adminRealmRep.setEventsListeners(Collections.singletonList(TestEventsListenerContextDetailsProviderFactory.PROVIDER_ID));
            testRealms.add(adminRealmRep);
        }
    }

    @After
    public void after() {
        testing().clearEventStore(realmId);
        testing().clearEventStore(realmId2);
    }

    @Test
    public void save() {
        testing().onEvent(create(EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
    }

    @Test
     // This looks like some database issue, test should get events which are newer or equal to requested time, however it gets only newer events from remote server
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        testing().onEvent(create(EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(newest, EventType.REGISTER, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(newest, EventType.REGISTER, realmId, "clientId", "userId2", "127.0.0.1", "error"));
        testing().onEvent(create(EventType.LOGIN, realmId2, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(oldest, EventType.LOGIN, realmId, "clientId2", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(EventType.LOGIN, realmId, "clientId", "userId2", "127.0.0.1", "error"));

        Assert.assertEquals(4, testing().queryEvents(realmId, null, "clientId", null, null, null, null, null, null).size());
        Assert.assertEquals(5, testing().queryEvents(realmId, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(3, testing().queryEvents(realmId, toList(EventType.LOGIN), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(realmId2, toList(EventType.LOGIN), null, null, null, null, null, null, null).size());
        Assert.assertEquals(5, testing().queryEvents(realmId, toList(EventType.LOGIN, EventType.REGISTER), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(realmId2, toList(EventType.LOGIN, EventType.REGISTER), null, null, null, null, null, null, null).size());
        Assert.assertEquals(3, testing().queryEvents(realmId, null, null, "userId", null, null, null, null, null).size());

        Assert.assertEquals(1, testing().queryEvents(realmId, toList(EventType.REGISTER), null, "userId", null, null, null, null, null).size());

        Assert.assertEquals(2, testing().queryEvents(realmId, null, null, null, null, null, null, null, 2).size());
        Assert.assertEquals(1, testing().queryEvents(realmId, null, null, null, null, null, null, 4, null).size());

        Assert.assertEquals(newest, testing().queryEvents(realmId, null, null, null, null, null, null, null, 1).get(0).getTime());
        Assert.assertEquals(oldest, testing().queryEvents(realmId, null, null, null, null, null, null, 4, 1).get(0).getTime());

        testing().clearEventStore(realmId);
        testing().clearEventStore(realmId2);

        Assert.assertEquals(0, testing().queryEvents(realmId, null, null, null, null, null, null, null, null).size());

        String d04 = "2015-03-04";
        String d05 = "2015-03-05";
        String d06 = "2015-03-06";
        String d07 = "2015-03-07";

        String d01 = "2015-03-01";
        String d03 = "2015-03-03";
        String d08 = "2015-03-08";
        String d10 = "2015-03-10";

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date04 = null, date05 = null, date06 = null, date07 = null;

        try {
            date04 = formatter.parse(d04);
            date05 = formatter.parse(d05);
            date06 = formatter.parse(d06);
            date07 = formatter.parse(d07);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        testing().onEvent(create(date04, EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(date04, EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(date05, EventType.REGISTER, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(date05, EventType.REGISTER, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(date06, EventType.CODE_TO_TOKEN, realmId, "clientId", "userId2", "127.0.0.1", "error"));
        testing().onEvent(create(date06, EventType.LOGOUT, realmId, "clientId", "userId2", "127.0.0.1", "error"));
        testing().onEvent(create(date07, EventType.UPDATE_PROFILE, realmId2, "clientId2", "userId2", "127.0.0.1", "error"));
        testing().onEvent(create(date07, EventType.UPDATE_EMAIL, realmId2, "clientId2", "userId2", "127.0.0.1", "error"));

        Assert.assertEquals(6, testing().queryEvents(realmId, null, "clientId", null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId2, null, "clientId2", null, null, null, null, null, null).size());

        Assert.assertEquals(6, testing().queryEvents(realmId, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId2, null, null, null, null, null, null, null, null).size());

        Assert.assertEquals(4, testing().queryEvents(realmId, null, null, "userId", null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId, null, null, "userId2", null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId2, null, null, "userId2", null, null, null, null, null).size());

        Assert.assertEquals(2, testing().queryEvents(realmId, toList(EventType.LOGIN), null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId, toList(EventType.REGISTER), null, null, null, null, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(realmId, toList(EventType.LOGIN, EventType.REGISTER), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(realmId, toList(EventType.CODE_TO_TOKEN), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(realmId, toList(EventType.LOGOUT), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(realmId2, toList(EventType.UPDATE_PROFILE), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(realmId2, toList(EventType.UPDATE_EMAIL), null, null, null, null, null, null, null).size());

        Assert.assertEquals(6, testing().queryEvents(realmId, null, null, null, d04, null, null, null, null).size());
        Assert.assertEquals(6, testing().queryEvents(realmId, null, null, null, null, d07, null, null, null).size());

        Assert.assertEquals(2, testing().queryEvents(realmId, null, null, null, d06, null, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(realmId, null, null, null, null, d05, null, null, null).size());

        Assert.assertEquals(0, testing().queryEvents(realmId2, null, null, null, d08, null, null, null, null).size());
        Assert.assertEquals(0, testing().queryEvents(realmId2, null, null, null, null, d03, null, null, null).size());

        Assert.assertEquals(6, testing().queryEvents(realmId, null, null, null, d04, d07, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId2, null, null, null, d04, d07, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(realmId, null, null, null, d05, d07, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId2, null, null, null, d05, d07, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(realmId, null, null, null, d04, d05, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId, null, null, null, d06, d07, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId2, null, null, null, d06, d07, null, null, null).size());

        Assert.assertEquals(0, testing().queryEvents(realmId, null, null, null, d01, d03, null, null, null).size());
        Assert.assertEquals(0, testing().queryEvents(realmId2, null, null, null, d01, d03, null, null, null).size());
        Assert.assertEquals(0, testing().queryEvents(realmId, null, null, null, d08, d10, null, null, null).size());
        Assert.assertEquals(0, testing().queryEvents(realmId2, null, null, null, d08, d10, null, null, null).size());
    }

    @Test
    public void testEventBuilder() {
        testingClient.server(REALM_NAME_1).run(session -> {
            RealmModel realm = session.getContext().getRealm();

            EventBuilder event = new EventBuilder(realm, session)
                    .event(EventType.LOGIN)
                    .session("session1")
                    .user("user1")
                    .client("client1");
            event.clone().error(Errors.USER_NOT_FOUND);

            event.clone().success();
        });

        // expect events to contain the realm name as detail (session context correctly set)
        assertEvents.expect(EventType.LOGIN_ERROR)
                .ipAddress(Matchers.blankOrNullString())
                .realm(realmId)
                .client("client1")
                .user("user1")
                .session("session1")
                .error(Errors.USER_NOT_FOUND)
                .detail(TestEventsListenerContextDetailsProviderFactory.CONTEXT_REALM_DETAIL, REALM_NAME_1)
                .assertEvent();
        assertEvents.expect(EventType.LOGIN)
                .ipAddress(Matchers.blankOrNullString())
                .realm(realmId)
                .client("client1")
                .user("user1")
                .session("session1")
                .detail(TestEventsListenerContextDetailsProviderFactory.CONTEXT_REALM_DETAIL, REALM_NAME_1)
                .assertEvent();

        // the two events should be retrieved from the store provider
        List<EventRepresentation> events = testing().queryEvents(realmId, null, null, null, null, null, null, null, null);
        Assert.assertEquals(2, events.size());
        EventRepresentation event = events.stream().filter(e -> EventType.LOGIN.toString().equals(e.getType())).findFirst().orElse(null);
        Assert.assertNotNull("No LOGIN event found", event);
        Assert.assertEquals("user1", event.getUserId());
        Assert.assertEquals("client1", event.getClientId());
        Assert.assertEquals("session1", event.getSessionId());
        event = events.stream().filter(e -> EventType.LOGIN_ERROR.toString().equals(e.getType())).findFirst().orElse(null);
        Assert.assertNotNull("No LOGIN_ERROR event found", event);
        Assert.assertEquals("user1", event.getUserId());
        Assert.assertEquals("client1", event.getClientId());
        Assert.assertEquals("session1", event.getSessionId());
    }

    @Test
    public void clear() {
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 20000, EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realmId2, "clientId", "userId", "127.0.0.1", "error"));

        testing().clearEventStore(realmId);

        Assert.assertEquals(0, testing().queryEvents(realmId, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(realmId2, null, null, null, null, null, null, null, null).size());
    }

    @Test
    public void lengthExceedLimit(){
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realmId, StringUtils.repeat("clientId", 100), "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, StringUtils.repeat(realmId, 100), "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realmId, "clientId", StringUtils.repeat("userId", 100), "127.0.0.1", "error"));

    }

    @Test
    public void maxLengthWithNull(){
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, realmId, null, null, "127.0.0.1", "error"));
    }

    @Test
    public void clearOld() {
        testing().onEvent(create(System.currentTimeMillis() - 300000, EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 200000, EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, realmId, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 300000, EventType.LOGIN, realmId2, "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, realmId2, "clientId", "userId", "127.0.0.1", "error"));

        // Set expiration of events for realmId .
        RealmRepresentation realm = realmsResouce().realm(REALM_NAME_1).toRepresentation();
        realm.setEventsExpiration(100);
        realmsResouce().realm(REALM_NAME_1).update(realm);

        // The first 2 events from realmId will be deleted
        testing().clearExpiredEvents();
        Assert.assertEquals(2, testing().queryEvents(realmId, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(realmId2, null, null, null, null, null, null, null, null).size());

        // Set expiration of events for realmId2 as well
        RealmRepresentation realm2 = realmsResouce().realm(REALM_NAME_2).toRepresentation();
        realm2.setEventsExpiration(100);
        realmsResouce().realm(REALM_NAME_2).update(realm2);

        // The first event from realmId2 will be deleted now
        testing().clearExpiredEvents();
        Assert.assertEquals(2, testing().queryEvents(realmId, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(realmId2, null, null, null, null, null, null, null, null).size());

        // set time offset to the future. The remaining 2 events from realmId and 1 event from realmId2 should be expired now
        setTimeOffset(150);
        testing().clearExpiredEvents();
        Assert.assertEquals(0, testing().queryEvents(realmId, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(0, testing().queryEvents(realmId2, null, null, null, null, null, null, null, null).size());

        // Revert expirations
        realm.setEventsExpiration(0);
        realmsResouce().realm(REALM_NAME_1).update(realm);
        realm2.setEventsExpiration(0);
        realmsResouce().realm(REALM_NAME_2).update(realm2);
    }

    private EventRepresentation create(EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        return create(System.currentTimeMillis(), event, realmId, clientId, userId, ipAddress, error);
    }

    private EventRepresentation create(Date date, EventType event, String realmId, String clientId, String userId, String ipAddress, String error) {
        return create(date.getTime(), event, realmId, clientId, userId, ipAddress, error);
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

}
