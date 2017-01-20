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
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test getting and filtering admin events.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class AdminEventTest extends AbstractEventTest {

    @Before
    public void initConfig() {
        enableEvents();
        testRealmResource().clearAdminEvents();
    }

    private List<AdminEventRepresentation> events() {
        return testRealmResource().getAdminEvents();
    }

    private String createUser(String username) {
        UserRepresentation user = createUserRepresentation(username, username + "@foo.com", "foo", "bar", true);
        return ApiUtil.createUserWithAdminClient(testRealmResource(), user);
    }

    private void updateRealm() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setDisplayName("Fury Road");
        testRealmResource().update(realm);
    }

    @Test
    public void clearAdminEventsTest() {
        createUser("user0");
        assertEquals(1, events().size());
        testRealmResource().clearAdminEvents();
        assertEquals(Collections.EMPTY_LIST, events());
    }

    @Test
    public void adminEventAttributeTest() {
        createUser("user5");
        List<AdminEventRepresentation> events = events();
        assertEquals(1, events.size());

        AdminEventRepresentation event = events.get(0);
        assertTrue(event.getTime() > 0);
        assertEquals(realmName(), event.getRealmId());
        assertEquals("CREATE", event.getOperationType());
        assertNotNull(event.getResourcePath());
        assertNull(event.getError());

        AuthDetailsRepresentation details = event.getAuthDetails();
        assertEquals(realmName(), details.getRealmId());
        assertNotNull(details.getClientId());
        assertNotNull(details.getUserId());
        assertNotNull(details.getIpAddress());
    }

    @Test
    public void retrieveAdminEventTest() {
        createUser("user1");
        List<AdminEventRepresentation> events = events();

        assertEquals(1, events.size());
        AdminEventRepresentation event = events().get(0);
        assertEquals("CREATE", event.getOperationType());

        assertEquals(realmName(), event.getRealmId());
        assertEquals(realmName(), event.getAuthDetails().getRealmId());
        assertNull(event.getRepresentation());
    }

    @Test
    public void testGetRepresentation() {
        configRep.setAdminEventsDetailsEnabled(Boolean.TRUE);
        saveConfig();

        createUser("user2");
        AdminEventRepresentation event = events().get(0);
        assertNotNull(event.getRepresentation());
        assertTrue(event.getRepresentation().contains("foo"));
        assertTrue(event.getRepresentation().contains("bar"));
    }

    @Test
    public void testFilterAdminEvents() {
        // two CREATE and one UPDATE
        createUser("user3");
        createUser("user4");
        updateRealm();
        assertEquals(3, events().size());

        List<AdminEventRepresentation> events = testRealmResource().getAdminEvents(Arrays.asList("CREATE"), realmName(), null, null, null, null, null, null, null, null);
        assertEquals(2, events.size());
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

        assertEquals(100, realm.getAdminEvents(null, null, null, null, null, null, null, null, null, null).size());
        assertEquals(105, realm.getAdminEvents(null, null, null, null, null, null, null, null, 0, 105).size());
        assertTrue(realm.getAdminEvents(null, null, null, null, null, null, null, null, 0, 1000).size() >= 110);
    }

}
