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

package org.keycloak.testsuite.admin.event;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 * Test getting and filtering admin events.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class AdminEventTest extends AbstractEventTest {

    private String masterRealmId;

    @Before
    public void initConfig() {
        enableEvents();
        testRealmResource().clearAdminEvents();
        this.masterRealmId = adminClient.realm(MASTER).toRepresentation().getId();
    }

    private List<AdminEventRepresentation> events() {
        return testRealmResource().getAdminEvents();
    }

    private String createUser(String username) {
        UserRepresentation user = createUserRepresentation(username, username + "@foo.com", "foo", "bar", true);
        String userId = ApiUtil.createUserWithAdminClient(testRealmResource(), user);
        getCleanup().addUserId(userId);
        return userId;
    }

    private void updateRealm() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setDisplayName("Fury Road");
        testRealmResource().update(realm);
    }

    @Test
    public void clearAdminEventsTest() {
        createUser("user0");
        assertThat(events().size(), is(equalTo(1)));
        testRealmResource().clearAdminEvents();
        assertThat(events(), is(empty()));
    }

    @Test
    public void adminEventAttributeTest() {
        createUser("user5");
        List<AdminEventRepresentation> events = events();
        assertThat(events().size(), is(equalTo(1)));

        AdminEventRepresentation event = events.get(0);
        assertThat(event.getTime(), is(greaterThan(0L)));
        assertThat(event.getRealmId(), is(equalTo(realmName())));
        assertThat(event.getOperationType(), is(equalTo("CREATE")));
        assertThat(event.getResourcePath(), is(notNullValue()));
        assertThat(event.getError(), is(nullValue()));

        AuthDetailsRepresentation details = event.getAuthDetails();
        assertThat(details.getRealmId(), is(equalTo(masterRealmId)));
        assertThat(details.getClientId(), is(notNullValue()));
        assertThat(details.getUserId(), is(notNullValue()));
        assertThat(details.getIpAddress(), is(notNullValue()));
    }

    @Test
    public void retrieveAdminEventTest() {
        createUser("user1");
        List<AdminEventRepresentation> events = events();

        assertThat(events.size(), is(equalTo(1)));
        AdminEventRepresentation event = events().get(0);
        assertThat(event.getOperationType(), is(equalTo("CREATE")));

        assertThat(event.getRealmId(), is(equalTo(realmName())));
        assertThat(event.getAuthDetails().getRealmId(), is(equalTo(masterRealmId)));
        assertThat(event.getRepresentation(), is(nullValue()));
    }

    @Test
    public void testGetRepresentation() {
        configRep.setAdminEventsDetailsEnabled(Boolean.TRUE);
        saveConfig();

        createUser("user2");
        AdminEventRepresentation event = events().stream()
                .filter(adminEventRep -> adminEventRep.getOperationType().equals("CREATE"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Wasn't able to obtain CREATE admin event."));
        assertThat(event.getRepresentation(), is(notNullValue()));
        assertThat(event.getRepresentation(), allOf(containsString("foo"), containsString("bar")));
    }

    @Test
    public void testFilterAdminEvents() {
        // two CREATE and one UPDATE
        createUser("user3");
        createUser("user4");
        updateRealm();
        assertThat(events().size(), is(equalTo(3)));

        List<AdminEventRepresentation> events = testRealmResource().getAdminEvents(Arrays.asList("CREATE"), null, null, null, null, null, null, null, null, null);
        assertThat(events.size(), is(equalTo(2)));
    }

    @Test
    public void defaultMaxResults() {
        RealmResource realm = adminClient.realms().realm("test");
        AdminEventRepresentation event = new AdminEventRepresentation();
        event.setOperationType(OperationType.CREATE.toString());
        event.setAuthDetails(new AuthDetailsRepresentation());
        event.setRealmId(realm.toRepresentation().getId());

        for (int i = 0; i < 110; i++) {
            testingClient.testing("test").onAdminEvent(event, false);
        }

        assertThat(realm.getAdminEvents(null, null, null, null, null, null, null, null, null, null).size(), is(equalTo(100)));
        assertThat(realm.getAdminEvents(null, null, null, null, null, null, null, null, 0, 105).size(), is(equalTo(105)));
        assertThat(realm.getAdminEvents(null, null, null, null, null, null, null, null, 0, 1000).size(), is(greaterThanOrEqualTo(110)));
    }

    private void checkUpdateRealmEventsConfigEvent(int size) {
        List<AdminEventRepresentation> events = events();
        assertThat(events.size(), is(equalTo(size)));
        
        AdminEventRepresentation event = events().get(0);
        assertThat(event.getOperationType(), is(equalTo("UPDATE")));
        assertThat(event.getRealmId(), is(equalTo(realmName())));
        assertThat(event.getResourcePath(), is(equalTo("events/config")));
        assertThat(event.getAuthDetails().getRealmId(), is(equalTo(masterRealmId)));
        assertThat(event.getRepresentation(), is(notNullValue()));
    }
    
    @Test
    public void updateRealmEventsConfig() {
        // change from OFF to ON should be stored
        configRep.setAdminEventsDetailsEnabled(Boolean.TRUE);
        configRep.setAdminEventsEnabled(Boolean.TRUE);
        saveConfig();
        checkUpdateRealmEventsConfigEvent(1);
        
        // any other change should be store too
        configRep.setEventsEnabled(Boolean.TRUE);
        saveConfig();
        checkUpdateRealmEventsConfigEvent(2);
        
        // change from ON to OFF should be stored too
        configRep.setAdminEventsEnabled(Boolean.FALSE);
        saveConfig();
        checkUpdateRealmEventsConfigEvent(3);
        
        // another change should not be stored cos it was OFF already
        configRep.setAdminEventsDetailsEnabled(Boolean.FALSE);
        saveConfig();
        assertThat(events().size(), is(equalTo(3)));
    }
}
