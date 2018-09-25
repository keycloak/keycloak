/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.crossdc;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.common.util.Retry;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InvalidationCrossDCTest extends AbstractAdminCrossDCTest {

    private static final String REALM_NAME = "test";

    @Test
    public void realmInvalidationTest() throws Exception {
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

        RealmRepresentation realmDc0 = getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME).toRepresentation();
        RealmRepresentation realmDc1 = getAdminClientForStartedNodeInDc(1).realms().realm(REALM_NAME).toRepresentation();

        // Test same realm on both DCs
        Assert.assertNull(realmDc0.getDisplayName());
        Assert.assertTrue(realmDc0.isRegistrationAllowed());
        Assert.assertNull(realmDc1.getDisplayName());
        Assert.assertTrue(realmDc1.isRegistrationAllowed());

        // Update realm on DC0
        realmDc0.setRegistrationAllowed(false);
        realmDc0.setDisplayName("Cool Realm!");
        getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME).update(realmDc0);

        // Assert updated on both DC0 and DC1 (here retry is needed. We need to wait until invalidation message arrives)
        realmDc0 = getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME).toRepresentation();
        Assert.assertEquals("Cool Realm!", realmDc0.getDisplayName());
        Assert.assertFalse(realmDc0.isRegistrationAllowed());

        AtomicInteger i = new AtomicInteger(0);
        Retry.execute(() -> {
            i.incrementAndGet();
            RealmRepresentation realmDcc1 = getAdminClientForStartedNodeInDc(1).realms().realm(REALM_NAME).toRepresentation();
            Assert.assertEquals("Cool Realm!", realmDcc1.getDisplayName());
            Assert.assertFalse(realmDcc1.isRegistrationAllowed());
        }, 50, 50);

        log.infof("realmInvalidationTest: Passed after '%d' iterations", i.get());
    }


    @Test
    public void clientInvalidationTest() throws Exception {
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

        ClientResource clientResourceDc0 = ApiUtil.findClientByClientId(getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME), "named-test-app");
        ClientResource clientResourceDc1 = ApiUtil.findClientByClientId(getAdminClientForStartedNodeInDc(1).realms().realm(REALM_NAME), "named-test-app");
        ClientRepresentation clientDc0 = clientResourceDc0.toRepresentation();
        ClientRepresentation clientDc1 = clientResourceDc1.toRepresentation();

        // Test same client on both DCs
        Assert.assertEquals("My Named Test App", clientDc0.getName());
        Assert.assertEquals("My Named Test App", clientDc1.getName());

        // Update client on DC0
        clientDc0.setName("Changed Test App");
        clientResourceDc0.update(clientDc0);

        // Assert updated on both DC0 and DC1 (here retry is needed. We need to wait until invalidation message arrives)
        clientDc0 = clientResourceDc0.toRepresentation();
        Assert.assertEquals("Changed Test App", clientDc0.getName());

        AtomicInteger i = new AtomicInteger(0);
        Retry.execute(() -> {
            i.incrementAndGet();
            ClientRepresentation clientDcc1 = clientResourceDc1.toRepresentation();
            Assert.assertEquals("Changed Test App", clientDcc1.getName());
        }, 50, 50);

        log.infof("clientInvalidationTest: Passed after '%d' iterations", i.get());
    }


    @Test
    public void clientListInvalidationTest() throws Exception {
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

        List<ClientRepresentation> dc0List = getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME).clients().findAll();
        List<ClientRepresentation> dc1List = getAdminClientForStartedNodeInDc(1).realms().realm(REALM_NAME).clients().findAll();


        // Test same clients on both DCs
        Assert.assertEquals(dc0List.size(), dc1List.size());
        int initialSize = dc0List.size();

        // Create client on DC0
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("some-new-client");
        rep.setEnabled(true);
        Response response = getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME).clients().create(rep);
        Assert.assertEquals(201, response.getStatus());
        response.close();

        // Assert updated on both DC0 and DC1 (here retry is needed. We need to wait until invalidation message arrives)
        dc0List = getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME).clients().findAll();
        Assert.assertEquals(initialSize + 1, dc0List.size());

        AtomicInteger i = new AtomicInteger(0);
        Retry.execute(() -> {
            i.incrementAndGet();
            List<ClientRepresentation> dc1Listt = getAdminClientForStartedNodeInDc(1).realms().realm(REALM_NAME).clients().findAll();
            Assert.assertEquals(initialSize + 1, dc1Listt.size());
        }, 50, 50);

        log.infof("clientListInvalidationTest: Passed after '%d' iterations", i.get());
    }


    @Test
    public void userInvalidationTest() throws Exception {
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

        UserResource userResourceDc0 = ApiUtil.findUserByUsernameId(getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME), "test-user@localhost");
        UserResource userResourceDc1 = ApiUtil.findUserByUsernameId(getAdminClientForStartedNodeInDc(1).realms().realm(REALM_NAME), "test-user@localhost");
        UserRepresentation userDc0 = userResourceDc0.toRepresentation();
        UserRepresentation userDc1 = userResourceDc1.toRepresentation();

        // Test same user on both DCs
        Assert.assertEquals("Tom", userDc0.getFirstName());
        Assert.assertEquals("Tom", userDc1.getFirstName());

        // Update user on DC0
        userDc0.setFirstName("Brad");
        userResourceDc0.update(userDc0);

        // Assert updated on both DC0 and DC1 (here retry is needed. We need to wait until invalidation message arrives)
        userDc0 = userResourceDc0.toRepresentation();
        Assert.assertEquals("Brad", userDc0.getFirstName());

        AtomicInteger i = new AtomicInteger(0);
        Retry.execute(() -> {
            i.incrementAndGet();
            UserRepresentation userDcc1 = userResourceDc1.toRepresentation();
            Assert.assertEquals("Brad", userDcc1.getFirstName());
        }, 50, 50);

        log.infof("userInvalidationTest: Passed after '%d' iterations", i.get());
    }


    @Test
    public void authzResourceInvalidationTest() throws Exception {
        enableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);


        ResourcesResource resourcesDc0Resource = ApiUtil.findClientByClientId(getAdminClientForStartedNodeInDc(0).realms().realm(REALM_NAME), "test-app-authz").authorization().resources();
        ResourcesResource resourcesDc1Resource = ApiUtil.findClientByClientId(getAdminClientForStartedNodeInDc(1).realms().realm(REALM_NAME), "test-app-authz").authorization().resources();
        ResourceRepresentation resDc0 = resourcesDc0Resource.findByName("Premium Resource").get(0);
        ResourceRepresentation resDc1 = resourcesDc1Resource.findByName("Premium Resource").get(0);

        // Test same resource on both DCs
        Assert.assertEquals("/protected/premium/*", resDc0.getUri());
        Assert.assertEquals("/protected/premium/*", resDc1.getUri());

        // Update resource on DC0
        resDc0.setUri("/protected/ultra/premium/*");
        resourcesDc0Resource.resource(resDc0.getId()).update(resDc0);

        // Assert updated on both DC0 and DC1 (here retry is needed. We need to wait until invalidation message arrives)
        resDc0 = resourcesDc0Resource.findByName("Premium Resource").get(0);
        Assert.assertEquals("/protected/ultra/premium/*", resDc0.getUri());

        AtomicInteger i = new AtomicInteger(0);
        Retry.execute(() -> {
            i.incrementAndGet();
            ResourceRepresentation ressDc1 = resourcesDc1Resource.findByName("Premium Resource").get(0);
            Assert.assertEquals("/protected/ultra/premium/*", ressDc1.getUri());
        }, 50, 50);

        log.infof("authzResourceInvalidationTest: Passed after '%d' iterations", i.get());
    }


}
