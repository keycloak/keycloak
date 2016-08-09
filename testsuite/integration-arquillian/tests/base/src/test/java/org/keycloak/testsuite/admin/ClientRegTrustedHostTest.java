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

import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientRegistrationTrustedHostResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ClientRegistrationTrustedHostRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminEventPaths;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegTrustedHostTest extends AbstractAdminTest {


    private ClientRegistrationTrustedHostResource resource;

    @Before
    public void before() {
        resource = realm.clientRegistrationTrustedHost();
    }

    @Test
    public void testInitialAccessTokens() {

        // Successfully create "localhost1" rep
        ClientRegistrationTrustedHostRepresentation rep = new ClientRegistrationTrustedHostRepresentation();
        rep.setHostName("localhost1");
        rep.setCount(5);

        Response res = resource.create(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientRegistrationTrustedHostPath("localhost1"), rep, ResourceType.CLIENT_REGISTRATION_TRUSTED_HOST_MODEL);
        res.close();

        // Failed to create conflicting rep "localhost1" again
        res = resource.create(rep);
        Assert.assertEquals(409, res.getStatus());
        assertAdminEvents.assertEmpty();
        res.close();

        // Successfully create "localhost2" rep
        rep = new ClientRegistrationTrustedHostRepresentation();
        rep.setHostName("localhost2");
        rep.setCount(10);

        res = resource.create(rep);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientRegistrationTrustedHostPath("localhost2"), rep, ResourceType.CLIENT_REGISTRATION_TRUSTED_HOST_MODEL);
        res.close();

        // Get "localhost1"
        rep = resource.get("localhost1");
        assertRep(rep, "localhost1", 5, 5);

        // Update "localhost1"
        rep.setCount(7);
        rep.setRemainingCount(7);
        resource.update("localhost1", rep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.clientRegistrationTrustedHostPath("localhost1"), rep, ResourceType.CLIENT_REGISTRATION_TRUSTED_HOST_MODEL);

        // Get all
        List<ClientRegistrationTrustedHostRepresentation> alls = resource.list();
        Assert.assertEquals(2, alls.size());
        assertRep(findByHost(alls, "localhost1"), "localhost1", 7, 7);
        assertRep(findByHost(alls, "localhost2"), "localhost2", 10, 10);

        // Delete "localhost1"
        resource.delete("localhost1");
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientRegistrationTrustedHostPath("localhost1"), ResourceType.CLIENT_REGISTRATION_TRUSTED_HOST_MODEL);

        // Get all and check just "localhost2" available
        alls = resource.list();
        Assert.assertEquals(1, alls.size());
        assertRep(alls.get(0), "localhost2", 10, 10);
    }

    private ClientRegistrationTrustedHostRepresentation findByHost(List<ClientRegistrationTrustedHostRepresentation> list, String hostName) {
        for (ClientRegistrationTrustedHostRepresentation rep : list) {
            if (hostName.equals(rep.getHostName())) {
                return rep;
            }
        }
        return null;
    }

    private void assertRep(ClientRegistrationTrustedHostRepresentation rep, String expectedHost, int expectedCount, int expectedRemaining) {
        Assert.assertEquals(expectedHost, rep.getHostName());
        Assert.assertEquals(expectedCount, rep.getCount().intValue());
        Assert.assertEquals(expectedRemaining, rep.getRemainingCount().intValue());
    }
}
