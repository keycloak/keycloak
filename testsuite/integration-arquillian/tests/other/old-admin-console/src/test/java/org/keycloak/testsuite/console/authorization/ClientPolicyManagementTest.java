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
package org.keycloak.testsuite.console.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.console.page.clients.authorization.policy.ClientPolicy;
import org.keycloak.testsuite.util.ClientBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ClientPolicyManagementTest extends AbstractAuthorizationSettingsTest {

    @Before
    public void configureTest() {
        super.configureTest();
        ClientsResource clients = testRealmResource().clients();
        clients.create(ClientBuilder.create().clientId("client a").build());
        clients.create(ClientBuilder.create().clientId("client b").build());
        clients.create(ClientBuilder.create().clientId("client c").build());
    }

    @Test
    public void testUpdate() throws InterruptedException {
        authorizationPage.navigateTo();
        ClientPolicyRepresentation expected = new ClientPolicyRepresentation();

        expected.setName("Test Client Policy");
        expected.setDescription("description");
        expected.addClient("client a");
        expected.addClient("client b");
        expected.addClient("client c");

        expected = createPolicy(expected);

        String previousName = expected.getName();

        expected.setName("Changed Test Client Policy");
        expected.setDescription("Changed description");
        expected.setLogic(Logic.NEGATIVE);

        expected.setClients(expected.getClients().stream().filter(client -> !client.equals("client b")).collect(Collectors.toSet()));

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        ClientPolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());

        assertPolicy(expected, actual);
    }

    @Test
    public void testDelete() throws InterruptedException {
        authorizationPage.navigateTo();
        ClientPolicyRepresentation expected = new ClientPolicyRepresentation();

        expected.setName("Test Client Policy");
        expected.setDescription("description");
        expected.addClient("client c");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().delete(expected.getName());
        assertAlertSuccess();
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    @Test
    public void testDeleteFromList() throws InterruptedException {
        authorizationPage.navigateTo();
        ClientPolicyRepresentation expected = new ClientPolicyRepresentation();

        expected.setName("Test Client Policy");
        expected.setDescription("description");
        expected.addClient("client c");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    private ClientPolicyRepresentation createPolicy(ClientPolicyRepresentation expected) {
        ClientPolicy policy = authorizationPage.authorizationTabs().policies().create(expected);
        assertAlertSuccess();
        return assertPolicy(expected, policy);
    }

    private ClientPolicyRepresentation assertPolicy(ClientPolicyRepresentation expected, ClientPolicy policy) {
        ClientPolicyRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getLogic(), actual.getLogic());

        assertNotNull(actual.getClients());
        assertEquals(expected.getClients().size(), actual.getClients().size());
        assertEquals(0, actual.getClients().stream().filter(actualClient -> !expected.getClients().stream()
                .filter(expectedClient -> actualClient.equals(expectedClient))
                .findFirst().isPresent())
                .count());
        return actual;
    }
}
