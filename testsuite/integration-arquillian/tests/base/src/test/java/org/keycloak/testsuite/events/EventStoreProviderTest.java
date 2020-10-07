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
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class EventStoreProviderTest extends AbstractEventsTest {

    @After
    public void after() {
        testing().clearEventStore();
    }

    @Test
    public void save() {
        testing().onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // This looks like some database issue, test should get events which are newer or equal to requested time, however it gets only newer events from remote server
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        testing().onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(newest, EventType.REGISTER, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(newest, EventType.REGISTER, "realmId", "clientId", "userId2", "127.0.0.1", "error"));
        testing().onEvent(create(EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(oldest, EventType.LOGIN, "realmId", "clientId2", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(EventType.LOGIN, "realmId", "clientId", "userId2", "127.0.0.1", "error"));

        Assert.assertEquals(5, testing().queryEvents(null, null, "clientId", null, null, null, null, null, null).size());
        Assert.assertEquals(5, testing().queryEvents("realmId", null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(null, toList(EventType.LOGIN), null, null, null, null, null, null, null).size());
        Assert.assertEquals(6, testing().queryEvents(null, toList(EventType.LOGIN, EventType.REGISTER), null, null, null, null, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(null, null, null, "userId", null, null, null, null, null).size());

        Assert.assertEquals(1, testing().queryEvents(null, toList(EventType.REGISTER), null, "userId", null, null, null, null, null).size());

        Assert.assertEquals(2, testing().queryEvents(null, null, null, null, null, null, null, null, 2).size());
        Assert.assertEquals(1, testing().queryEvents(null, null, null, null, null, null, null, 5, null).size());

        Assert.assertEquals(newest, testing().queryEvents(null, null, null, null, null, null, null, null, 1).get(0).getTime());
        Assert.assertEquals(oldest, testing().queryEvents(null, null, null, null, null, null, null, 5, 1).get(0).getTime());

        testing().clearEventStore("realmId");
        testing().clearEventStore("realmId2");

        Assert.assertEquals(0, testing().queryEvents(null, null, null, null, null, null, null, null, null).size());

        String d1 = "2015-03-04";
        String d2 = "2015-03-05";
        String d3 = "2015-03-06";
        String d4 = "2015-03-07";

        String d5 = "2015-03-01";
        String d6 = "2015-03-03";
        String d7 = "2015-03-08";
        String d8 = "2015-03-10";

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = null, date2 = null, date3 = null, date4 = null;

        try {
            date1 = formatter.parse(d1);
            date2 = formatter.parse(d2);
            date3 = formatter.parse(d3);
            date4 = formatter.parse(d4);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        testing().onEvent(create(date1, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(date1, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(date2, EventType.REGISTER, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(date2, EventType.REGISTER, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(date3, EventType.CODE_TO_TOKEN, "realmId", "clientId", "userId2", "127.0.0.1", "error"));
        testing().onEvent(create(date3, EventType.LOGOUT, "realmId", "clientId", "userId2", "127.0.0.1", "error"));
        testing().onEvent(create(date4, EventType.UPDATE_PROFILE, "realmId2", "clientId2", "userId2", "127.0.0.1", "error"));
        testing().onEvent(create(date4, EventType.UPDATE_EMAIL, "realmId2", "clientId2", "userId2", "127.0.0.1", "error"));

        Assert.assertEquals(6, testing().queryEvents(null, null, "clientId", null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(null, null, "clientId2", null, null, null, null, null, null).size());

        Assert.assertEquals(6, testing().queryEvents("realmId", null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents("realmId2", null, null, null, null, null, null, null, null).size());

        Assert.assertEquals(4, testing().queryEvents(null, null, null, "userId", null, null, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(null, null, null, "userId2", null, null, null, null, null).size());

        Assert.assertEquals(2, testing().queryEvents(null, toList(EventType.LOGIN), null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().queryEvents(null, toList(EventType.REGISTER), null, null, null, null, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(null, toList(EventType.LOGIN, EventType.REGISTER), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(null, toList(EventType.CODE_TO_TOKEN), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(null, toList(EventType.LOGOUT), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(null, toList(EventType.UPDATE_PROFILE), null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().queryEvents(null, toList(EventType.UPDATE_EMAIL), null, null, null, null, null, null, null).size());

        Assert.assertEquals(8, testing().queryEvents(null, null, null, null, d1, null, null, null, null).size());
        Assert.assertEquals(8, testing().queryEvents(null, null, null, null, null, d4, null, null, null).size());

        Assert.assertEquals(4, testing().queryEvents(null, null, null, null, d3, null, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(null, null, null, null, null, d2, null, null, null).size());

        Assert.assertEquals(0, testing().queryEvents(null, null, null, null, d7, null, null, null, null).size());
        Assert.assertEquals(0, testing().queryEvents(null, null, null, null, null, d6, null, null, null).size());

        Assert.assertEquals(8, testing().queryEvents(null, null, null, null, d1, d4, null, null, null).size());
        Assert.assertEquals(6, testing().queryEvents(null, null, null, null, d2, d4, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(null, null, null, null, d1, d2, null, null, null).size());
        Assert.assertEquals(4, testing().queryEvents(null, null, null, null, d3, d4, null, null, null).size());

        Assert.assertEquals(0, testing().queryEvents(null, null, null, null, d5, d6, null, null, null).size());
        Assert.assertEquals(0, testing().queryEvents(null, null, null, null, d7, d8, null, null, null).size());
    }

    @Test
    public void clear() {
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 20000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));

        testing().clearEventStore("realmId");

        Assert.assertEquals(1, testing().queryEvents(null, null, null, null, null, null, null, null, null).size());
    }

    @Test
    public void lengthExceedLimit(){
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", StringUtils.repeat("clientId", 100), "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, StringUtils.repeat("realmId", 100), "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", "clientId", StringUtils.repeat("userId", 100), "127.0.0.1", "error"));

    }

    @Test
    public void maxLengthWithNull(){
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, null, null, null, "127.0.0.1", "error"));
    }

    @Test
    public void clearOld() {
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 20000, EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis(), EventType.LOGIN, "realmId", "clientId", "userId", "127.0.0.1", "error"));
        testing().onEvent(create(System.currentTimeMillis() - 30000, EventType.LOGIN, "realmId2", "clientId", "userId", "127.0.0.1", "error"));

        testing().clearEventStore("realmId", System.currentTimeMillis() - 10000);

        Assert.assertEquals(3, testing().queryEvents(null, null, null, null, null, null, null, null, null).size());
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
