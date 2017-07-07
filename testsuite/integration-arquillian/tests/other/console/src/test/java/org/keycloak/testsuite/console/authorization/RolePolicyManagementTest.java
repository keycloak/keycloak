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

import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.policy.RolePolicy;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyManagementTest extends AbstractAuthorizationSettingsTest {

    @Before
    public void configureTest() {
        super.configureTest();
        RolesResource realmRoles = testRealmResource().roles();
        realmRoles.create(new RoleRepresentation("Realm Role A", "", false));
        realmRoles.create(new RoleRepresentation("Realm Role B", "", false));
        realmRoles.create(new RoleRepresentation("Realm Role C", "", false));
        RolesResource clientRoles = testRealmResource().clients().get(newClient.getId()).roles();
        clientRoles.create(new RoleRepresentation("Client Role A", "", false));
        clientRoles.create(new RoleRepresentation("Client Role B", "", false));
        clientRoles.create(new RoleRepresentation("Client Role C", "", false));
    }

    @Test
    public void testUpdateRealmRoles() throws InterruptedException {
        authorizationPage.navigateTo();
        RolePolicyRepresentation expected = new RolePolicyRepresentation();

        expected.setName("Test Update Realm Role Policy");
        expected.setDescription("description");
        expected.addRole("Realm Role A");
        expected.addRole("Realm Role B");
        expected.addRole("Realm Role C");

        expected = createPolicy(expected);

        String previousName = expected.getName();

        expected.setName("Changed Test Realm Role Policy");
        expected.setDescription("Changed description");
        expected.setLogic(Logic.NEGATIVE);

        expected.setRoles(expected.getRoles().stream().filter(roleDefinition -> !roleDefinition.getId().equals("Realm Role B")).collect(Collectors.toSet()));

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        RolePolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());
        expected = assertPolicy(expected, actual);

        expected.getRoles().iterator().next().setRequired(true);

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();
        expected = assertPolicy(expected, actual);

        expected.getRoles().clear();
        expected.addRole("Realm Role B", true);

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();
        assertPolicy(expected, actual);
    }

    @Test
    public void testUpdateClientRoles() throws InterruptedException {
        authorizationPage.navigateTo();
        RolePolicyRepresentation expected = new RolePolicyRepresentation();

        expected.setName("Test Update Client Role Policy");
        expected.setDescription("description");

        String clientId = newClient.getClientId();

        expected.addClientRole(clientId, "Client Role A");
        expected.addClientRole(clientId, "Client Role B");
        expected.addClientRole(clientId, "Client Role C");

        expected = createPolicy(expected);

        String previousName = expected.getName();

        expected.setName("Changed Test Update Client Role Policy");
        expected.setDescription("Changed description");

        expected.setRoles(expected.getRoles().stream().filter(roleDefinition -> !roleDefinition.getId().contains("Client Role B")).collect(Collectors.toSet()));

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        RolePolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());
        expected = assertPolicy(expected, actual);

        expected.getRoles().iterator().next().setRequired(true);

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();
        expected = assertPolicy(expected, actual);

        expected.getRoles().clear();
        expected.addClientRole(clientId, "Client Role B", true);

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();
        assertPolicy(expected, actual);
    }

    @Test
    public void testRealmAndClientRoles() throws InterruptedException {
        authorizationPage.navigateTo();
        RolePolicyRepresentation expected = new RolePolicyRepresentation();

        expected.setName("Test Realm And Client Role Policy");
        expected.setDescription("description");

        String clientId = newClient.getClientId();

        expected.addRole("Realm Role A");
        expected.addRole("Realm Role C");
        expected.addClientRole(clientId, "Client Role A");
        expected.addClientRole(clientId, "Client Role B");
        expected.addClientRole(clientId, "Client Role C");

        expected = createPolicy(expected);
        expected.setRoles(expected.getRoles().stream().filter(roleDefinition -> !roleDefinition.getId().contains("Client Role B") && !roleDefinition.getId().contains("Realm Role A")).collect(Collectors.toSet()));

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        RolePolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());
        expected = assertPolicy(expected, actual);

        expected.getRoles().forEach(roleDefinition -> {
            if (roleDefinition.getId().equals("Realm Role C")) {
                roleDefinition.setRequired(true);
            }
        });

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();
        expected = assertPolicy(expected, actual);

        expected.getRoles().clear();
        expected.addClientRole(clientId, "Client Role B", true);

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();
        assertPolicy(expected, actual);
    }

    @Test
    public void testDelete() throws InterruptedException {
        authorizationPage.navigateTo();
        RolePolicyRepresentation expected = new RolePolicyRepresentation();

        expected.setName("Test Delete Role Policy");
        expected.setDescription("description");
        expected.addRole("Realm Role A");
        expected.addRole("Realm Role B");
        expected.addRole("Realm Role C");

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
        RolePolicyRepresentation expected = new RolePolicyRepresentation();

        expected.setName("Test Delete Role Policy");
        expected.setDescription("description");
        expected.addRole("Realm Role A");
        expected.addRole("Realm Role B");
        expected.addRole("Realm Role C");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    private RolePolicyRepresentation createPolicy(RolePolicyRepresentation expected) {
        RolePolicy policy = authorizationPage.authorizationTabs().policies().create(expected);
        assertAlertSuccess();
        return assertPolicy(expected, policy);
    }

    private RolePolicyRepresentation assertPolicy(RolePolicyRepresentation expected, RolePolicy policy) {
        RolePolicyRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getLogic(), actual.getLogic());

        assertNotNull(actual.getRoles());
        assertEquals(expected.getRoles().size(), actual.getRoles().size());
        assertEquals(0, actual.getRoles().stream().filter(actualDefinition -> !expected.getRoles().stream()
                .filter(roleDefinition -> actualDefinition.getId().contains(roleDefinition.getId().indexOf("/") != -1 ? roleDefinition.getId().split("/")[1] : roleDefinition.getId()) && actualDefinition.isRequired() == roleDefinition.isRequired())
                .findFirst().isPresent())
                .count());
        return actual;
    }
}
