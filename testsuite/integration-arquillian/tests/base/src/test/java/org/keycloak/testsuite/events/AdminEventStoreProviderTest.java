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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.keycloak.events.admin.OperationType;
import org.keycloak.models.jpa.entities.RealmAttributes;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class AdminEventStoreProviderTest extends AbstractEventsTest {

    @After
    public void after() {
        testing().clearAdminEventStore(realmId);
        testing().clearAdminEventStore(realmId2);
    }

    @Test
    public void save() {
        testing().onAdminEvent(create(realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
    }

    @Test
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        testing().onAdminEvent(create(realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(newest, realmId, OperationType.ACTION, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(newest, realmId, OperationType.ACTION, realmId, "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(realmId2, OperationType.CREATE, realmId2, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(oldest, realmId, OperationType.CREATE, realmId, "clientId2", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(realmId, OperationType.CREATE, realmId, "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        Assert.assertEquals(4, testing().getAdminEvents(realmId, null, null, "clientId", null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, "clientId", null, null, null, null, null, null, null).size());
        Assert.assertEquals(5, testing().getAdminEvents(realmId, null, realmId, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(3, testing().getAdminEvents(realmId, toList(OperationType.CREATE), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, toList(OperationType.CREATE), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(5, testing().getAdminEvents(realmId, toList(OperationType.CREATE, OperationType.ACTION), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, toList(OperationType.CREATE, OperationType.ACTION), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(3, testing().getAdminEvents(realmId, null, null, null, "userId", null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, "userId", null, null, null, null, null, null).size());

        Assert.assertEquals(1, testing().getAdminEvents(realmId, toList(OperationType.ACTION), null, null, "userId", null, null, null, null, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId2, toList(OperationType.ACTION), null, null, "userId", null, null, null, null, null, null).size());

        Assert.assertEquals(2, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, null, 2).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, 4, null).size());

        Assert.assertEquals(newest, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, null, 1).get(0).getTime());
        Assert.assertEquals(oldest, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, 4, 1).get(0).getTime());

        testing().clearAdminEventStore(realmId);
        testing().clearAdminEventStore(realmId2);

        Assert.assertEquals(0, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId2, null, null, null, null, null, null, null, null, null, null).size());

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

        testing().onAdminEvent(create(date04, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(date04, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(date05, realmId, OperationType.ACTION, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(date05, realmId, OperationType.ACTION, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(date06, realmId, OperationType.UPDATE, realmId, "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(date06, realmId, OperationType.DELETE, realmId, "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(date07, realmId2, OperationType.CREATE, realmId2, "clientId2", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(date07, realmId2, OperationType.CREATE, realmId2, "clientId2", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        Assert.assertEquals(6, testing().getAdminEvents(realmId, null, null, "clientId", null, null, null, null, null, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId2, null, null, "clientId", null, null, null, null, null, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId, null, null, "clientId2", null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, null, "clientId2", null, null, null, null, null, null, null).size());

        Assert.assertEquals(6, testing().getAdminEvents(realmId, null, realmId, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, realmId2, null, null, null, null, null, null, null, null).size());

        Assert.assertEquals(4, testing().getAdminEvents(realmId, null, null, null, "userId", null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId, null, null, null, "userId2", null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, null, null, "userId2", null, null, null, null, null, null).size());

        Assert.assertEquals(2, testing().getAdminEvents(realmId, toList(OperationType.ACTION), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(4, testing().getAdminEvents(realmId, toList(OperationType.CREATE, OperationType.ACTION), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, toList(OperationType.CREATE, OperationType.ACTION), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId, toList(OperationType.UPDATE), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId, toList(OperationType.DELETE), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId, toList(OperationType.CREATE), null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, toList(OperationType.CREATE), null, null, null, null, null, null, null, null, null).size());

        Assert.assertEquals(6, testing().getAdminEvents(realmId, null, null, null, null, null, null, d04, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, null, null, null, null, null, d04, null, null, null).size());
        Assert.assertEquals(6, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, d07, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, null, null, null, null, null, null, d07, null, null).size());

        Assert.assertEquals(2, testing().getAdminEvents(realmId, null, null, null, null, null, null, d06, null, null, null).size());
        Assert.assertEquals(4, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, d05, null, null).size());

        Assert.assertEquals(0, testing().getAdminEvents(realmId2, null, null, null, null, null, null, d08, null, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId2, null, null, null, null, null, null, null, d03, null, null).size());

        Assert.assertEquals(6, testing().getAdminEvents(realmId, null, null, null, null, null, null, d04, d07, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, null, null, null, null, null, d04, d07, null, null).size());
        Assert.assertEquals(4, testing().getAdminEvents(realmId, null, null, null, null, null, null, d05, d07, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, null, null, null, null, null, d05, d07, null, null).size());
        Assert.assertEquals(4, testing().getAdminEvents(realmId, null, null, null, null, null, null, d04, d05, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId, null, null, null, null, null, null, d06, d07, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, null, null, null, null, null, d06, d07, null, null).size());

        Assert.assertEquals(0, testing().getAdminEvents(realmId, null, null, null, null, null, null, d01, d03, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId2, null, null, null, null, null, null, d01, d03, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId, null, null, null, null, null, null, d08, d10, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId2, null, null, null, null, null, null, d08, d10, null, null).size());

    }

    @Test
    public void queryResourcePath() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        testing().onAdminEvent(create(realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(newest, realmId, OperationType.ACTION, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(newest, realmId, OperationType.ACTION, realmId, "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(realmId2, OperationType.CREATE, realmId2, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(oldest, realmId, OperationType.CREATE, realmId, "clientId2", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(realmId, OperationType.CREATE, realmId, "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        Assert.assertEquals(5, testing().getAdminEvents(realmId, null, null, null, null, null, "/admin/*", null, null, null, null).size());
        Assert.assertEquals(5, testing().getAdminEvents(realmId, null, null, null, null, null, "*/realms/*", null, null, null, null).size());
        Assert.assertEquals(5, testing().getAdminEvents(realmId, null, null, null, null, null, "*/master", null, null, null, null).size());
        Assert.assertEquals(5, testing().getAdminEvents(realmId, null, null, null, null, null, "/admin/realms/*", null, null, null, null).size());
        Assert.assertEquals(5, testing().getAdminEvents(realmId, null, null, null, null, null, "*/realms/master", null, null, null, null).size());
        Assert.assertEquals(5, testing().getAdminEvents(realmId, null, null, null, null, null, "/admin/*/master", null, null, null, null).size());
        Assert.assertEquals(5, testing().getAdminEvents(realmId, null, null, null, null, null, "/ad*/*/master", null, null, null, null).size());

        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, "/admin/*", null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, "*/realms/*", null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, "*/master", null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, "/admin/realms/*", null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, "*/realms/master", null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, "/admin/*/master", null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, "/ad*/*/master", null, null, null, null).size());
    }

    @Test
    public void clear() {
        testing().onAdminEvent(create(System.currentTimeMillis() - 30000, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis() - 20000, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis(), realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis(), realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis() - 30000, realmId2, OperationType.CREATE, realmId2, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        testing().clearAdminEventStore(realmId);

        Assert.assertEquals(0, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, null, null, null, null, null).size());
    }

    @Test
    public void clearOld() {
        testing().onAdminEvent(create(System.currentTimeMillis() - 30000, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis() - 20000, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis(), realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis(), realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis() - 30000, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        testing().clearAdminEventStore(realmId, System.currentTimeMillis() - 10000);

        Assert.assertEquals(2, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, null, null).size());
    }

    @Test
    public void expireOld() {
        testing().onAdminEvent(create(System.currentTimeMillis() - 30000, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis() - 20000, realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis(), realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis(), realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis() - 30000, realmId2, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        testing().onAdminEvent(create(System.currentTimeMillis(), realmId2, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        // Set expiration of events for realmId .
        RealmRepresentation realm = realmsResouce().realm(REALM_NAME_1).toRepresentation();
        Map<String, String> attributes = realm.getAttributes();
        attributes.put(RealmAttributes.ADMIN_EVENTS_EXPIRATION,"10");
        realm.setAttributes(attributes);
        realmsResouce().realm(REALM_NAME_1).update(realm);

        // The first 2 events from realmId will be deleted
        testing().clearExpiredEvents();
        Assert.assertEquals(2, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(2, testing().getAdminEvents(realmId2, null, null, null, null, null, null, null, null, null, null).size());

        // Set expiration of events for realmId2 as well
        RealmRepresentation realm2 = realmsResouce().realm(REALM_NAME_2).toRepresentation();
        Map<String, String> attributes2 = realm2.getAttributes();
        attributes2.put(RealmAttributes.ADMIN_EVENTS_EXPIRATION,"10");
        realm2.setAttributes(attributes2);
        realmsResouce().realm(REALM_NAME_2).update(realm2);

        // The first event from realmId2 will be deleted now
        testing().clearExpiredEvents();
        Assert.assertEquals(2, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(1, testing().getAdminEvents(realmId2, null, null, null, null, null, null, null, null, null, null).size());

        // set time offset to the future. The remaining 2 events from realmId and 1 event from realmId2 should be expired now
        setTimeOffset(150);
        testing().clearExpiredEvents();
        Assert.assertEquals(0, testing().getAdminEvents(realmId, null, null, null, null, null, null, null, null, null, null).size());
        Assert.assertEquals(0, testing().getAdminEvents(realmId2, null, null, null, null, null, null, null, null, null, null).size());

        // Revert expirations
        attributes.put(RealmAttributes.ADMIN_EVENTS_EXPIRATION,"0");
        realm.setAttributes(attributes);
        realmsResouce().realm(REALM_NAME_1).update(realm);
        attributes2.put(RealmAttributes.ADMIN_EVENTS_EXPIRATION,"0");
        realm2.setAttributes(attributes2);
        realmsResouce().realm(REALM_NAME_2).update(realm2);
    }

    @Test
    public void handleCustomResourceTypeEvents() {
        testing().onAdminEvent(create(realmId, OperationType.CREATE, realmId, "clientId", "userId", "127.0.0.1", "/admin/realms/master", "my-custom-resource", "error"), false);

        List<AdminEventRepresentation> adminEvents = testing().getAdminEvents(realmId, null, null, "clientId", null, null, null, null, null, null, null);
        Assert.assertEquals(1, adminEvents.size());
        Assert.assertEquals("my-custom-resource", adminEvents.get(0).getResourceType());
    }

    private AdminEventRepresentation create(String realmId, OperationType operation, String authRealmId, String authClientId, String authUserId, String authIpAddress, String resourcePath, String error) {
        return create(System.currentTimeMillis(), realmId, operation, authRealmId, authClientId, authUserId, authIpAddress, resourcePath, error);
    }

    private AdminEventRepresentation create(String realmId, OperationType operation, String authRealmId, String authClientId, String authUserId, String authIpAddress, String resourcePath, String resourceType, String error) {
        return create(System.currentTimeMillis(), realmId, operation, authRealmId, authClientId, authUserId, authIpAddress, resourcePath, resourceType, error);
    }

    private AdminEventRepresentation create(Date date, String realmId, OperationType operation, String authRealmId, String authClientId, String authUserId, String authIpAddress, String resourcePath, String error) {
        return create(date.getTime(), realmId, operation, authRealmId, authClientId, authUserId, authIpAddress, resourcePath, error);
    }

    private AdminEventRepresentation create(long time, String realmId, OperationType operation, String authRealmId, String authClientId, String authUserId, String authIpAddress, String resourcePath, String error) {
        return create(time, realmId, operation, authRealmId, authClientId, authUserId, authIpAddress, resourcePath, null, error);
    }

    private AdminEventRepresentation create(long time, String realmId, OperationType operation, String authRealmId, String authClientId, String authUserId, String authIpAddress, String resourcePath, String resourceType, String error) {
        AdminEventRepresentation e = new AdminEventRepresentation();
        e.setTime(time);
        e.setRealmId(realmId);
        e.setOperationType(operation.toString());
        AuthDetailsRepresentation authDetails = new AuthDetailsRepresentation();
        authDetails.setRealmId(authRealmId);
        authDetails.setClientId(authClientId);
        authDetails.setUserId(authUserId);
        authDetails.setIpAddress(authIpAddress);
        e.setAuthDetails(authDetails);
        e.setResourcePath(resourcePath);
        e.setResourceType(resourceType);
        e.setError(error);

        return e;
    }

}
