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

package org.keycloak.testsuite.admin;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientInitialAccessResource;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.Time;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.util.AdminEventPaths;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InitialAccessTokenResourceTest extends AbstractAdminTest {

    private ClientInitialAccessResource resource;

    @Before
    public void before() {
        resource = realm.clientInitialAccess();
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // Time difference is possible on remote server
    public void testInitialAccessTokens() {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(2);
        rep.setExpiration(100);

        int time = Time.currentTime();

        ClientInitialAccessPresentation response = resource.create(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientInitialAccessPath(response.getId()), rep, ResourceType.CLIENT_INITIAL_ACCESS_MODEL);

        assertNotNull(response.getId());
        assertEquals(new Integer(2), response.getCount());
        assertEquals(new Integer(2), response.getRemainingCount());
        assertEquals(new Integer(100), response.getExpiration());
        assertThat(response.getTimestamp(), allOf(greaterThanOrEqualTo(time), lessThanOrEqualTo(Time.currentTime())));
        assertNotNull(response.getToken());

        rep.setCount(3);
        response = resource.create(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientInitialAccessPath(response.getId()), rep, ResourceType.CLIENT_INITIAL_ACCESS_MODEL);

        rep.setCount(4);
        response = resource.create(rep);
        String lastId = response.getId();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientInitialAccessPath(lastId), rep, ResourceType.CLIENT_INITIAL_ACCESS_MODEL);

        List<ClientInitialAccessPresentation> list = resource.list();
        assertEquals(3, list.size());

        assertEquals(9, list.get(0).getCount() + list.get(1).getCount() + list.get(2).getCount());
        assertNull(list.get(0).getToken());

        // Delete last and assert it was deleted
        resource.delete(lastId);
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientInitialAccessPath(lastId), ResourceType.CLIENT_INITIAL_ACCESS_MODEL);

        list = resource.list();
        assertEquals(2, list.size());
        assertEquals(5, list.get(0).getCount() + list.get(1).getCount());
    }


    @Test
    public void testPeriodicExpiration() throws ClientRegistrationException, InterruptedException {
        ClientInitialAccessPresentation response1 = resource.create(new ClientInitialAccessCreatePresentation(1, 1));
        ClientInitialAccessPresentation response2 = resource.create(new ClientInitialAccessCreatePresentation(1000, 1));
        ClientInitialAccessPresentation response3 = resource.create(new ClientInitialAccessCreatePresentation(1000, 0));
        ClientInitialAccessPresentation response4 = resource.create(new ClientInitialAccessCreatePresentation(0, 1));

        List<ClientInitialAccessPresentation> list = resource.list();
        assertEquals(4, list.size());

        setTimeOffset(10);

        testingClient.testing().removeExpired(REALM_NAME);

        list = resource.list();
        assertEquals(2, list.size());

        List<String> remainingIds = list.stream()
                .map(initialAccessPresentation -> initialAccessPresentation.getId())
                .collect(Collectors.toList());

        Assert.assertNames(remainingIds, response2.getId(), response4.getId());

        setTimeOffset(2000);

        testingClient.testing().removeExpired(REALM_NAME);

        list = resource.list();
        assertEquals(1, list.size());
        Assert.assertEquals(list.get(0).getId(), response4.getId());

        // Cleanup
        realm.clientInitialAccess().delete(response4.getId());
    }

}
