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
import java.util.Date;
import java.util.List;

import org.keycloak.events.admin.OperationType;
import org.keycloak.models.jpa.entities.RealmAttributes;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminEventStoreProviderTest {

    @InjectRealm(config = AdminEventStoreProviderTest.AdminEventStoreRealm.class)
    ManagedRealm realm1;

    @InjectRealm(ref = "realm2", config = AdminEventStoreProviderTest.AdminEventStoreRealm.class)
    ManagedRealm realm2;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    EventRunOnServerHelper eventHelper;

    @BeforeEach
    public void setup() {
        eventHelper = new EventRunOnServerHelper(runOnServer);
        eventHelper.clearAdminEvents(realm1.getId(), realm2.getId());
    }

    @AfterEach
    public void after() {
        eventHelper.clearAdminEvents(realm1.getId(), realm2.getId());
    }

    @Test
    public void save() {
        eventHelper.storeEvent(create(realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
    }

    @Test
    public void query() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        eventHelper.storeEvent(create(realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(newest, realm1.getId(), OperationType.ACTION, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(newest, realm1.getId(), OperationType.ACTION, realm1.getId(), "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(realm2.getId(), OperationType.CREATE, realm2.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(oldest, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId2", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        Assertions.assertEquals(4, eventHelper.queryAdminEvents(realm1.getId(), null, null, "clientId", null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, "clientId", null, null, null, null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), null, realm1.getId(), null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(3, eventHelper.queryAdminEvents(realm1.getId(), List.of(OperationType.CREATE), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), List.of(OperationType.CREATE), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), List.of(OperationType.CREATE, OperationType.ACTION), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), List.of(OperationType.CREATE, OperationType.ACTION), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(3, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, "userId", null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, "userId", null, null, null, null, null, null).length);

        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm1.getId(), List.of(OperationType.ACTION), null, null, "userId", null, null, null, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm2.getId(), List.of(OperationType.ACTION), null, null, "userId", null, null, null, null, null, null).length);

        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, null, 2).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, 4, null).length);

        Assertions.assertEquals(newest, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, null, 1)[0].getTime());
        Assertions.assertEquals(oldest, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, 4, 1)[0].getTime());

        eventHelper.clearAdminEvents(realm1.getId(), realm2.getId());

        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, null, null, null, null).length);

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

        eventHelper.storeEvent(create(date04, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(date04, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(date05, realm1.getId(), OperationType.ACTION, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(date05, realm1.getId(), OperationType.ACTION, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(date06, realm1.getId(), OperationType.UPDATE, realm1.getId(), "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(date06, realm1.getId(), OperationType.DELETE, realm1.getId(), "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(date07, realm2.getId(), OperationType.CREATE, realm2.getId(), "clientId2", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(date07, realm2.getId(), OperationType.CREATE, realm2.getId(), "clientId2", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        Assertions.assertEquals(6, eventHelper.queryAdminEvents(realm1.getId(), null, null, "clientId", null, null, null, null, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm2.getId(), null, null, "clientId", null, null, null, null, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm1.getId(), null, null, "clientId2", null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, "clientId2", null, null, null, null, null, null, null).length);

        Assertions.assertEquals(6, eventHelper.queryAdminEvents(realm1.getId(), null, realm1.getId(), null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, realm2.getId(), null, null, null, null, null, null, null, null).length);

        Assertions.assertEquals(4, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, "userId", null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, "userId2", null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, "userId2", null, null, null, null, null, null).length);

        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm1.getId(), List.of(OperationType.ACTION), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(4, eventHelper.queryAdminEvents(realm1.getId(), List.of(OperationType.CREATE, OperationType.ACTION), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), List.of(OperationType.CREATE, OperationType.ACTION), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm1.getId(), List.of(OperationType.UPDATE), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm1.getId(), List.of(OperationType.DELETE), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm1.getId(), List.of(OperationType.CREATE), null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), List.of(OperationType.CREATE), null, null, null, null, null, null, null, null, null).length);

        Assertions.assertEquals(6, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, d04, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, d04, null, null, null).length);
        Assertions.assertEquals(6, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, d07, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, null, d07, null, null).length);

        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, d06, null, null, null).length);
        Assertions.assertEquals(4, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, d05, null, null).length);

        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, d08, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, null, d03, null, null).length);

        Assertions.assertEquals(6, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, d04, d07, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, d04, d07, null, null).length);
        Assertions.assertEquals(4, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, d05, d07, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, d05, d07, null, null).length);
        Assertions.assertEquals(4, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, d04, d05, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, d06, d07, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, d06, d07, null, null).length);

        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, d01, d03, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, d01, d03, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, d08, d10, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, d08, d10, null, null).length);

    }

    @Test
    public void queryResourcePath() {
        long oldest = System.currentTimeMillis() - 30000;
        long newest = System.currentTimeMillis() + 30000;

        eventHelper.storeEvent(create(realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(newest, realm1.getId(), OperationType.ACTION, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(newest, realm1.getId(), OperationType.ACTION, realm1.getId(), "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(realm2.getId(), OperationType.CREATE, realm2.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(oldest, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId2", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId2", "127.0.0.1", "/admin/realms/master", "error"), false);

        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, "/admin/*", null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, "*/realms/*", null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, "*/master", null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, "/admin/realms/*", null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, "*/realms/master", null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, "/admin/*/master", null, null, null, null).length);
        Assertions.assertEquals(5, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, "/ad*/*/master", null, null, null, null).length);

        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, "/admin/*", null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, "*/realms/*", null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, "*/master", null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, "/admin/realms/*", null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, "*/realms/master", null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, "/admin/*/master", null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, "/ad*/*/master", null, null, null, null).length);
    }

    @Test
    public void clear() {
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis() - 20000, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis(), realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis(), realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, realm2.getId(), OperationType.CREATE, realm2.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        eventHelper.clearAdminEvents(realm1.getId());

        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(1, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, null, null, null, null).length);
    }

    @Test
    public void clearOld() {
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis() - 20000, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis(), realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis(), realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        eventHelper.clearAdminEvents(realm1.getId(), System.currentTimeMillis() - 10000);
        eventHelper.clearAdminEvents(realm2.getId(), System.currentTimeMillis() - 10000);

        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, null, null).length);
    }

    @Test
    public void expireOld() {
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis() - 20000, realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis(), realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis(), realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis() - 30000, realm2.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);
        eventHelper.storeEvent(create(System.currentTimeMillis(), realm2.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "error"), false);

        // Set expiration of events for realm1.getId() .
        realm1.updateWithCleanup(r -> {
            r.attribute(RealmAttributes.ADMIN_EVENTS_EXPIRATION, "10");
            return r;
        });

        // The first 2 events from realm1.getId() will be deleted
        eventHelper.clearExpiredEvents();

        Assertions.assertEquals(3, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, null, null, null, null).length);

        // Set expiration of events for realm2.getId() as well
        realm2.updateWithCleanup(r -> {
            r.attribute(RealmAttributes.ADMIN_EVENTS_EXPIRATION, "10");
            return r;
        });

        // The first event from realm2.getId() will be deleted now
        eventHelper.clearExpiredEvents();
        Assertions.assertEquals(3, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(2, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, null, null, null, null).length);

        // set time offset to the future. The remaining 2 events from realm1.getId() and 1 event from realm2.getId() should be expired now
        timeOffSet.set(150);
        eventHelper.clearExpiredEvents();
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm1.getId(), null, null, null, null, null, null, null, null, null, null).length);
        Assertions.assertEquals(0, eventHelper.queryAdminEvents(realm2.getId(), null, null, null, null, null, null, null, null, null, null).length);
    }

    @Test
    public void handleCustomResourceTypeEvents() {
        eventHelper.storeEvent(create(realm1.getId(), OperationType.CREATE, realm1.getId(), "clientId", "userId", "127.0.0.1", "/admin/realms/master", "my-custom-resource", "error"), false);

        AdminEventRepresentation[] adminEvents = eventHelper.queryAdminEvents(realm1.getId(), null, null, "clientId", null, null, null, null, null, null, null);
        Assertions.assertEquals(1, adminEvents.length);
        Assertions.assertEquals("my-custom-resource", adminEvents[0].getResourceType());
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

    private static final class AdminEventStoreRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.adminEventsEnabled(true);
        }
    }
}
