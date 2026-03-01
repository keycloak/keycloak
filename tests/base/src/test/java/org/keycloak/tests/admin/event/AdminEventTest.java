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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Test getting and filtering admin events.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class AdminEventTest {

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @InjectRealm(config = AdminEventRealmConfig.class, ref = "default")
    ManagedRealm managedRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectRunOnServer(realmRef = "default")
    RunOnServerClient runOnServerClient;


    @BeforeEach
    public void initConfig() {
        managedRealm.admin().clearAdminEvents();
    }

    private List<AdminEventRepresentation> events() {
        return managedRealm.admin().getAdminEvents();
    }

    private String createUser(String username) {
        UserRepresentation user = UserConfigBuilder.create()
                .username(username)
                .email(username + "@foo.com")
                .name("foo", "bar")
                .password("password")
                .build();
        String userUuid = ApiUtil.getCreatedId(managedRealm.admin().users().create(user));
        managedRealm.cleanup().add(r -> r.users().get(userUuid).remove());
        return userUuid;
    }

    @Test
    public void clearAdminEventsTest() {
        createUser("user0");
        Assertions.assertEquals(1, events().size());
        managedRealm.admin().clearAdminEvents();
        Assertions.assertEquals(0, events().size());
    }

    @Test
    public void adminEventAttributeTest() {
        createUser("user5");
        List<AdminEventRepresentation> events = events();
        Assertions.assertEquals(1, events.size());

        AdminEventRepresentation event = events.get(0);
        AdminEventAssertion.assertSuccess(event).operationType(OperationType.CREATE);
        Assertions.assertTrue(event.getTime() > 0L);
        Assertions.assertEquals(managedRealm.getId(), event.getRealmId());
        Assertions.assertNotNull(event.getResourcePath());

        AuthDetailsRepresentation details = event.getAuthDetails();
        Assertions.assertEquals(masterRealm.getId(), details.getRealmId());
        Assertions.assertNotNull(details.getClientId());
        Assertions.assertNotNull(details.getUserId());
        Assertions.assertNotNull(details.getIpAddress());
    }

    @Test
    public void testEventDetails() {
        String userUuid = createUser("user5");
        UserRepresentation userRep = managedRealm.admin().users().get(userUuid).toRepresentation();

        managedRealm.updateWithCleanup(r -> r.organizationsEnabled(true));
        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName("test-org");
        orgRep.setAlias(orgRep.getName());
        orgRep.addDomain(new OrganizationDomainRepresentation(orgRep.getName()));
        managedRealm.admin().organizations().create(orgRep).close();
        orgRep = managedRealm.admin().organizations().list(-1, -1).get(0);
        managedRealm.admin().organizations().get(orgRep.getId()).members().addMember(userUuid).close();
        String orgId = orgRep.getId();
        managedRealm.cleanup().add(r -> r.organizations().get(orgId).delete());

        List<AdminEventRepresentation> events = events();
        Assertions.assertEquals(4, events.size());

        AdminEventRepresentation event = events.get(0);
        AdminEventAssertion.assertSuccess(event)
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.ORGANIZATION_MEMBERSHIP);
        Assertions.assertEquals(managedRealm.getId(), event.getRealmId());
        assertThat(event.getDetails(), is(equalTo(Map.of(UserModel.USERNAME, userRep.getUsername(), UserModel.EMAIL, userRep.getEmail()))));
    }

    @Test
    public void retrieveAdminEventTest() {
        createUser("user1");
        List<AdminEventRepresentation> events = events();

        Assertions.assertEquals(1, events.size());
        AdminEventRepresentation event = events().get(0);
        AdminEventAssertion.assertSuccess(event).operationType(OperationType.CREATE);

        Assertions.assertEquals(managedRealm.getId(), event.getRealmId());
        Assertions.assertEquals(masterRealm.getId(), event.getAuthDetails().getRealmId());
        Assertions.assertNull(event.getRepresentation());
    }

    @Test
    public void testGetRepresentation() {
        managedRealm.updateWithCleanup(r -> r.adminEventsDetailsEnabled(true));

        createUser("user2");
        AdminEventRepresentation event = events().stream()
                .filter(adminEventRep -> adminEventRep.getOperationType().equals("CREATE"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Wasn't able to obtain CREATE admin event."));
        Assertions.assertNotNull(event.getRepresentation());
        assertThat(event.getRepresentation(), allOf(containsString("foo"), containsString("bar")));
    }

    @Test
    public void testFilterAdminEvents() {
        // two CREATE and one UPDATE
        createUser("user3");
        createUser("user4");
        managedRealm.updateWithCleanup(r -> r.displayName("Fury Road"));
        Assertions.assertEquals(3, events().size());

        List<AdminEventRepresentation> events = managedRealm.admin().getAdminEvents(List.of("CREATE"), null, null, null, null, null, null, null, null, null);
        Assertions.assertEquals(2, events.size());
    }

    @Test
    public void defaultMaxResults() {
        RealmResource realm = managedRealm.admin();
        String realmId = managedRealm.getId();

        runOnServerClient.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);

            AdminEvent event = new AdminEvent();
            event.setOperationType(OperationType.CREATE);
            event.setAuthDetails(new AuthDetails());
            event.setRealmId(realmId);

            for (int i = 0; i < 110; i++) {
                event.setId(UUID.randomUUID().toString());
                provider.onEvent(event, false);
            }
        });

        Assertions.assertEquals(100,
                realm.getAdminEvents(null, null, null, null, null, null, null, null, null, null).size());
        Assertions.assertEquals(105,
                realm.getAdminEvents(null, null, null, null, null, null, null, null, 0, 105).size());
        Assertions.assertEquals(110,
                realm.getAdminEvents(null, null, null, null, null, null, null, null, 0, 1000).size());
    }

    @Test
    public void adminEventRepresentationLenght() {
        RealmResource realm = managedRealm.admin();
        String realmId = managedRealm.getId();
        String longValue = RandomStringUtils.secure().next(30000, true, true);

        runOnServerClient.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);

            AdminEvent event = new AdminEvent();
            event.setOperationType(OperationType.CREATE);
            event.setAuthDetails(new AuthDetails());
            event.setRealmId(realmId);
            event.setRepresentation(longValue);

            provider.onEvent(event, true);
        });

        List<AdminEventRepresentation> adminEvents = realm.getAdminEvents(null, null, null, null, null, null, null, null, null, null);

        Assertions.assertEquals(1, adminEvents.size());
        Assertions.assertEquals(longValue, adminEvents.get(0).getRepresentation());
    }

    @Test
    public void orderResultsTest() {
        RealmResource realm = managedRealm.admin();
        String realmId = managedRealm.getId();

        runOnServerClient.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);

            AdminEvent firstEvent = new AdminEvent();
            firstEvent.setOperationType(OperationType.CREATE);
            firstEvent.setAuthDetails(new AuthDetails());
            firstEvent.setRealmId(realmId);
            firstEvent.setTime(System.currentTimeMillis() - 1000);

            AdminEvent secondEvent = new AdminEvent();
            secondEvent.setOperationType(OperationType.DELETE);
            secondEvent.setAuthDetails(new AuthDetails());
            secondEvent.setRealmId(realmId);
            secondEvent.setTime(System.currentTimeMillis());

            provider.onEvent(firstEvent, false);
            provider.onEvent(secondEvent, false);
        });

        List<AdminEventRepresentation> adminEvents = realm.getAdminEvents(null, null, null, null, null, null, null, null, null, null, null, "desc");
        Assertions.assertEquals(2, adminEvents.size());
        Assertions.assertEquals(OperationType.DELETE.toString(), adminEvents.get(0).getOperationType());
        Assertions.assertEquals(OperationType.CREATE.toString(), adminEvents.get(1).getOperationType());

        adminEvents = realm.getAdminEvents(null, null, null, null, null, null, null, null, null, null, null, "asc");
        Assertions.assertEquals(2, adminEvents.size());
        Assertions.assertEquals(OperationType.DELETE.toString(), adminEvents.get(1).getOperationType());
        Assertions.assertEquals(OperationType.CREATE.toString(), adminEvents.get(0).getOperationType());
    }

    @Test
    public void filterByEpochTimeStamp() {
        RealmResource realm = managedRealm.admin();

        String realmId = managedRealm.getId();
        long currentTime = System.currentTimeMillis();

        runOnServerClient.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);

            AdminEvent event = new AdminEvent();
            event.setOperationType(OperationType.CREATE);
            event.setAuthDetails(new AuthDetails());
            event.setRealmId(realmId);

            event.setTime(currentTime - 2*24*3600*1000);
            provider.onEvent(event, false);
            event.setTime(currentTime - 1000);
            provider.onEvent(event, false);
            event.setTime(currentTime);
            provider.onEvent(event, false);
            event.setTime(currentTime + 1000);
            provider.onEvent(event, false);
            event.setTime(currentTime + 2*24*3600*1000);
            provider.onEvent(event, false);
        });

        List<AdminEventRepresentation> events = realm.getAdminEvents();
        Assertions.assertEquals(5, events.size());
        events = realm.getAdminEvents(null, null, null, null, null, null, null, currentTime, currentTime, null, null, null);
        Assertions.assertEquals(1, events.size());
        events = realm.getAdminEvents(null, null, null, null, null, null, null, currentTime - 1000, currentTime + 1000, null, null, null);
        Assertions.assertEquals(3, events.size());

        LocalDate dateFrom = Instant.ofEpochMilli(currentTime - 1000).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate dateTo = Instant.ofEpochMilli(currentTime + 1000).atZone(ZoneOffset.UTC).toLocalDate();
        events = realm.getAdminEvents(null, null, null, null, null, null, null, dateFrom.toString(), dateTo.toString(), null, null, null);
        Assertions.assertEquals(3, events.size());
    }

    private void checkUpdateRealmEventsConfigEvent(int size) {
        List<AdminEventRepresentation> events = events();
        Assertions.assertEquals(size, events.size());

        AdminEventRepresentation event = events.get(0);
        AdminEventAssertion.assertSuccess(event)
                .operationType(OperationType.UPDATE)
                .resourcePath("events", "config");

        Assertions.assertEquals(managedRealm.getId(), event.getRealmId());
        Assertions.assertEquals(masterRealm.getId(), event.getAuthDetails().getRealmId());
        Assertions.assertNotNull(event.getRepresentation());
    }

    private RealmEventsConfigRepresentation updateEventsConfig(RealmEventsConfigRepresentation configRep) {
        managedRealm.admin().updateRealmEventsConfig(configRep);
        return managedRealm.admin().getRealmEventsConfig();
    }

    @Test
    public void updateRealmEventsConfig() {
        // change from OFF to ON should be stored
        RealmEventsConfigRepresentation configRep = managedRealm.admin().getRealmEventsConfig();
        configRep.setAdminEventsDetailsEnabled(true);
        configRep.setAdminEventsEnabled(true);
        configRep = updateEventsConfig(configRep);
        checkUpdateRealmEventsConfigEvent(1);

        // any other change should be stored too
        configRep.setEventsEnabled(true);
        configRep = updateEventsConfig(configRep);
        checkUpdateRealmEventsConfigEvent(2);

        // change from ON to OFF should be stored too
        configRep.setAdminEventsEnabled(false);
        configRep = updateEventsConfig(configRep);
        checkUpdateRealmEventsConfigEvent(3);

        // another change should not be stored cos it was OFF already
        configRep.setAdminEventsDetailsEnabled(false);
        configRep = updateEventsConfig(configRep);
        assertThat(events().size(), is(equalTo(3)));

        // clean up the realm
        configRep.setAdminEventsEnabled(true);
        updateEventsConfig(configRep);
    }

    @Test
    public void createAndDeleteRealm() {
        // Enable admin events on "master" realm, since realm create/delete events will be stored in realm of
        // the authenticated user who executes the operations (admin in master realm),
        RealmEventsConfigRepresentation masterConfig = masterRealm.admin().getRealmEventsConfig();
        masterConfig.setAdminEventsDetailsEnabled(true);
        masterConfig.setAdminEventsEnabled(true);
        masterRealm.admin().updateRealmEventsConfig(masterConfig);
        masterRealm.admin().clearAdminEvents();

        // Create realm.
        String testRealm = "test-realm";
        RealmRepresentation realm = new RealmRepresentation();
        realm.setId(testRealm);
        realm.setRealm(testRealm);
        adminClient.realms().create(realm);

        // Delete realm.
        adminClient.realm(realm.getRealm()).remove();

        // Check that events were logged.
        List<AdminEventRepresentation> events = masterRealm.admin().getAdminEvents();
        Assertions.assertEquals(2, events.size());

        AdminEventRepresentation createEvent = events.get(1);
        AdminEventAssertion.assertSuccess(createEvent)
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.REALM)
                .resourcePath(testRealm);
        Assertions.assertEquals(masterRealm.getId(), createEvent.getRealmId());
        Assertions.assertNotNull(createEvent.getRepresentation());

        AdminEventRepresentation deleteEvent = events.get(0);
        AdminEventAssertion.assertSuccess(deleteEvent)
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.REALM)
                .resourcePath(testRealm);
        Assertions.assertEquals(masterRealm.getId(), deleteEvent.getRealmId());
    }

    @Test
    public void testStripOutUserSensitiveData() throws IOException {
        managedRealm.updateWithCleanup(r -> {
            r.adminEventsDetailsEnabled(true);
            r.adminEventsEnabled(true);
            return r;
        });

        UserResource user = managedRealm.admin().users().get(createUser("sensitive"));
        List<AdminEventRepresentation> events = events();
        UserRepresentation eventUserRep = JsonSerialization.readValue(events.get(0).getRepresentation(), UserRepresentation.class);
        Assertions.assertNull(eventUserRep.getCredentials());

        UserRepresentation userRep = user.toRepresentation();
        userRep = UserConfigBuilder.update(userRep).password("password").build();
        user.update(userRep);
        events = events();
        eventUserRep = JsonSerialization.readValue(events.get(0).getRepresentation(), UserRepresentation.class);
        Assertions.assertNull(eventUserRep.getCredentials());
    }

    private static class AdminEventRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.eventsEnabled(true)
                    .adminEventsEnabled(true)
                    .adminEventsDetailsEnabled(false);
        }
    }
}
