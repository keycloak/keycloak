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
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.RolePoliciesResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.permission.ResourcePermission;
import org.keycloak.testsuite.console.page.clients.authorization.permission.ScopePermission;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopePermissionManagementTest extends AbstractAuthorizationSettingsTest {

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
        RolePoliciesResource roles = policies.roles();

        roles.create(policyA);

        RolePolicyRepresentation policyB = new RolePolicyRepresentation();

        policyB.setName("Policy B");
        policyB.addRole("Role B");

        roles.create(policyB);

        UserPolicyRepresentation policyC = new UserPolicyRepresentation();

        policyC.setName("Policy C");
        policyC.addUser("test");

        policies.users().create(policyC);

        authorization.scopes().create(new ScopeRepresentation("Scope A"));
        authorization.scopes().create(new ScopeRepresentation("Scope B"));
        authorization.scopes().create(new ScopeRepresentation("Scope C"));

        ResourcesResource resources = authorization.resources();

        resources.create(new ResourceRepresentation("Resource A", "Scope A"));
        resources.create(new ResourceRepresentation("Resource B", "Scope B", "Scope C"));
    }

    @Test
    public void testUpdateScopeOnly() throws InterruptedException {
        authorizationPage.navigateTo();
        ScopePermissionRepresentation expected = new ScopePermissionRepresentation();

        expected.setName("Test Scope Only Permission");
        expected.setDescription("description");
        expected.addScope("Scope C", "Scope A", "Scope B");
        expected.addPolicy("Policy C", "Policy A", "Policy B");

        expected = createPermission(expected);

        String previousName = expected.getName();

        expected.setName(previousName + "Changed");
        expected.setDescription("changed");
        expected.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        expected.getScopes().clear();
        expected.addScope("Scope B");
        expected.getPolicies().clear();
        expected.addPolicy("Policy C");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        ScopePermission actual = authorizationPage.authorizationTabs().permissions().name(expected.getName());
        assertPolicy(expected, actual);
    }

    @Test
    public void testUpdateResourceScope() throws InterruptedException {
        authorizationPage.navigateTo();
        ScopePermissionRepresentation expected = new ScopePermissionRepresentation();

        expected.setName("Test Resource Scope Permission");
        expected.setDescription("description");
        expected.addResource("Resource A");
        expected.addScope("Scope A");
        expected.addPolicy("Policy C", "Policy A", "Policy B");

        expected = createPermission(expected);

        String previousName = expected.getName();

        expected.setName(previousName + "Changed");
        expected.setDescription("changed");
        expected.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        expected.getResources().clear();
        expected.addResource("Resource B");
        expected.getScopes().clear();
        expected.addScope("Scope B", "Scope C");
        expected.getPolicies().clear();
        expected.addPolicy("Policy C");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        ScopePermission actual = authorizationPage.authorizationTabs().permissions().name(expected.getName());
        assertPolicy(expected, actual);
    }

    @Test
    public void testDelete() throws InterruptedException {
        authorizationPage.navigateTo();
        ScopePermissionRepresentation expected = new ScopePermissionRepresentation();

        expected.setName("Test Delete Scope Permission");
        expected.setDescription("description");
        expected.addScope("Scope C");
        expected.addPolicy("Policy C");

        expected = createPermission(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().permissions().delete(expected.getName());
        assertAlertSuccess();
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().permissions().permissions().findByName(expected.getName()));
    }

    private ScopePermissionRepresentation createPermission(ScopePermissionRepresentation expected) {
        ScopePermission policy = authorizationPage.authorizationTabs().permissions().create(expected);
        assertAlertSuccess();
        return assertPolicy(expected, policy);
    }

    private ScopePermissionRepresentation assertPolicy(ScopePermissionRepresentation expected, ScopePermission policy) {
        ScopePermissionRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getDecisionStrategy(), actual.getDecisionStrategy());

        assertEquals(expected.getPolicies().size(), actual.getPolicies().size());
        assertEquals(0, actual.getPolicies().stream().filter(actualPolicy -> !expected.getPolicies().stream()
                .filter(expectedPolicy -> actualPolicy.equals(expectedPolicy))
                .findFirst().isPresent())
                .count());

        if (expected.getResources() != null) {
            assertEquals(expected.getResources().size(), actual.getResources().size());
            assertEquals(0, actual.getResources().stream().filter(actualResource -> !expected.getResources().stream()
                    .filter(expectedResource -> actualResource.equals(expectedResource))
                    .findFirst().isPresent())
                    .count());
        }

        assertEquals(expected.getScopes().size(), actual.getScopes().size());
        assertEquals(0, actual.getScopes().stream().filter(actualScope -> !expected.getScopes().stream()
                .filter(expectedScope -> actualScope.equals(expectedScope))
                .findFirst().isPresent())
                .count());

        return actual;
    }
}
