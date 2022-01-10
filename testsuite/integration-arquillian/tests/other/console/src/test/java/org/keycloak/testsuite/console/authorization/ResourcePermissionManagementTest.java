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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.ResourcePermissionsResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.RolePoliciesResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.permission.ResourcePermission;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePermissionManagementTest extends AbstractAuthorizationSettingsTest {

    @Before
    public void configureTest() {
        super.configureTest();
        RolesResource realmRoles = testRealmResource().roles();
        realmRoles.create(new RoleRepresentation("Role A", "", false));
        realmRoles.create(new RoleRepresentation("Role B", "", false));

        RolePolicyRepresentation policyA = new RolePolicyRepresentation();

        policyA.setName("Policy A");
        policyA.addRole("Role A");

        AuthorizationResource authorization = testRealmResource().clients().get(newClient.getId()).authorization();
        PoliciesResource policies = authorization.policies();
        RolePoliciesResource roles = policies.role();

        roles.create(policyA);

        RolePolicyRepresentation policyB = new RolePolicyRepresentation();

        policyB.setName("Policy B");
        policyB.addRole("Role B");

        roles.create(policyB);

        UserPolicyRepresentation policyC = new UserPolicyRepresentation();

        policyC.setName("Policy C");
        policyC.addUser("test");

        policies.user().create(policyC).close();

        ResourcesResource resources = authorization.resources();

        resources.create(new ResourceRepresentation("Resource A"));
        resources.create(new ResourceRepresentation("Resource B"));
    }

    @Test
    public void testCreateWithoutPolicies() throws InterruptedException {
        authorizationPage.navigateTo();
        ResourcePermissionRepresentation expected = new ResourcePermissionRepresentation();

        expected.setName("testCreateWithoutPolicies Permission");
        expected.setDescription("description");
        expected.addResource("Resource A");

        expected = createPermission(expected);

        authorizationPage.navigateTo();
        ResourcePermission actual = authorizationPage.authorizationTabs().permissions().name(expected.getName());
        assertPolicy(expected, actual);
    }

    @Test
    public void testUpdateResource() throws InterruptedException {
        authorizationPage.navigateTo();
        ResourcePermissionRepresentation expected = new ResourcePermissionRepresentation();

        expected.setName("testUpdateResource Permission");
        expected.setDescription("description");
        expected.addResource("Resource A");
        expected.addPolicy("Policy A");
        expected.addPolicy("Policy B");
        expected.addPolicy("Policy C");

        expected = createPermission(expected);

        String previousName = expected.getName();

        expected.setName(expected.getName() + " Changed");
        expected.setDescription("Changed description");
        expected.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        expected.getResources().clear();
        expected.addResource("Resource B");
        expected.getPolicies().clear();
        expected.addPolicy("Policy A", "Policy C");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        ResourcePermission actual = authorizationPage.authorizationTabs().permissions().name(expected.getName());
        assertPolicy(expected, actual);

        expected.getPolicies().clear();

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().update(expected.getName(), expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        actual = authorizationPage.authorizationTabs().permissions().name(expected.getName());
        assertPolicy(expected, actual);
    }

    @Test
    public void testUpdateResourceType() throws InterruptedException {
        authorizationPage.navigateTo();
        ResourcePermissionRepresentation expected = new ResourcePermissionRepresentation();

        expected.setName("testUpdateResourceType Permission");
        expected.setDescription("description");
        expected.setResourceType("test-resource-type");
        expected.addPolicy("Policy A");
        expected.addPolicy("Policy B");
        expected.addPolicy("Policy C");

        expected = createPermission(expected);

        String previousName = expected.getName();

        expected.setName(expected.getName() + " Changed");
        expected.setDescription("Changed description");
        expected.setDecisionStrategy(DecisionStrategy.CONSENSUS);

        expected.setResourceType("changed-resource-type");
        expected.setPolicies(expected.getPolicies().stream().filter(policy -> !policy.equals("Policy B")).collect(Collectors.toSet()));

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        ResourcePermission actual = authorizationPage.authorizationTabs().permissions().name(expected.getName());

        assertPolicy(expected, actual);
        
        expected.setResourceType(null);
        expected.addResource("Resource A");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().update(expected.getName(), expected);
        assertAlertSuccess();

        ResourcePermissionsResource resourcePermission = testRealmResource().clients().get(newClient.getId()).authorization()
                .permissions().resource();
        ResourcePermissionRepresentation permission = resourcePermission.findByName(expected.getName());
        
        assertFalse(resourcePermission.findById(permission.getId()).resources().isEmpty());

        expected.setResourceType("test");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().update(expected.getName(), expected);
        assertAlertSuccess();

        assertTrue(resourcePermission.findById(permission.getId()).resources().isEmpty());
    }

    @Test
    public void testDelete() throws InterruptedException {
        authorizationPage.navigateTo();
        ResourcePermissionRepresentation expected = new ResourcePermissionRepresentation();

        expected.setName("testDelete Permission");
        expected.setDescription("description");
        expected.addResource("Resource B");
        expected.addPolicy("Policy C");

        expected = createPermission(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().delete(expected.getName());
        assertAlertSuccess();
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().permissions().permissions().findByName(expected.getName()));
    }

    @Test
    public void testDeleteFromList() throws InterruptedException {
        authorizationPage.navigateTo();
        ResourcePermissionRepresentation expected = new ResourcePermissionRepresentation();

        expected.setName("testDeleteFromList Permission");
        expected.setDescription("description");
        expected.addResource("Resource B");
        expected.addPolicy("Policy C");

        expected = createPermission(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().permissions().permissions().findByName(expected.getName()));
    }

    @Test
    public void testCreateWithChild() {
        ResourcePermissionRepresentation expected = new ResourcePermissionRepresentation();

        expected.setName(UUID.randomUUID().toString());
        expected.setDescription("description");
        expected.addResource("Resource B");
        expected.addPolicy("Policy C");

        ResourcePermission policy = authorizationPage.authorizationTabs().permissions().create(expected, false);

        RolePolicyRepresentation childPolicy = new RolePolicyRepresentation();

        childPolicy.setName(UUID.randomUUID().toString());
        childPolicy.addRole("Role A");

        policy.createPolicy(childPolicy);
        policy.form().save();

        assertAlertSuccess();

        expected.addPolicy(childPolicy.getName());

        authorizationPage.navigateTo();
        ResourcePermission actual = authorizationPage.authorizationTabs().permissions().name(expected.getName());
        assertPolicy(expected, actual);
    }

    private ResourcePermissionRepresentation createPermission(ResourcePermissionRepresentation expected) {
        ResourcePermission policy = authorizationPage.authorizationTabs().permissions().create(expected, true);
        assertAlertSuccess();
        return assertPolicy(expected, policy);
    }

    private ResourcePermissionRepresentation assertPolicy(ResourcePermissionRepresentation expected, ResourcePermission policy) {
        ResourcePermissionRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getDecisionStrategy(), actual.getDecisionStrategy());
        assertEquals(expected.getResourceType(), actual.getResourceType());
        if (expected.getPolicies() == null) {
            assertTrue(actual.getPolicies() == null || actual.getPolicies().isEmpty());
        } else {
            assertEquals(expected.getPolicies().size(), actual.getPolicies().size());
        }
        assertEquals(0, actual.getPolicies().stream().filter(actualPolicy -> !expected.getPolicies().stream()
                .filter(expectedPolicy -> actualPolicy.equals(expectedPolicy))
                .findFirst().isPresent())
                .count());
        assertEquals(0, actual.getResources().stream().filter(actualResource -> !expected.getResources().stream()
                .filter(expectedResource -> actualResource.equals(expectedResource))
                .findFirst().isPresent())
                .count());

        return actual;
    }
}
