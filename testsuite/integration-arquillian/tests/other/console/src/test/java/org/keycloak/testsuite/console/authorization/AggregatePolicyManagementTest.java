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
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.admin.client.resource.RolePoliciesResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.policy.AggregatePolicy;
import org.keycloak.testsuite.console.page.clients.authorization.policy.UserPolicy;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AggregatePolicyManagementTest extends AbstractAuthorizationSettingsTest {

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
    }

    @Test
    public void testUpdate() throws InterruptedException {
        authorizationPage.navigateTo();
        AggregatePolicyRepresentation expected = new AggregatePolicyRepresentation();

        expected.setName("Test Update Aggregate Policy");
        expected.setDescription("description");
        expected.addPolicy("Policy A");
        expected.addPolicy("Policy B");
        expected.addPolicy("Policy C");

        expected = createPolicy(expected);

        String previousName = expected.getName();

        expected.setName("Changed Test Update Aggregate Policy");
        expected.setDescription("Changed description");
        expected.setLogic(Logic.NEGATIVE);
        expected.getPolicies().clear();
        expected.addPolicy("Policy A", "Policy C");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        AggregatePolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());

        assertPolicy(expected, actual);
    }

    @Test
    public void testDelete() throws InterruptedException {
        authorizationPage.navigateTo();
        AggregatePolicyRepresentation expected = new AggregatePolicyRepresentation();

        expected.setName("Test Delete Aggregate Policy");
        expected.setDescription("description");
        expected.addPolicy("Policy C");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().delete(expected.getName());
        assertAlertSuccess();
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    private AggregatePolicyRepresentation createPolicy(AggregatePolicyRepresentation expected) {
        AggregatePolicy policy = authorizationPage.authorizationTabs().policies().create(expected);
        assertAlertSuccess();
        return assertPolicy(expected, policy);
    }

    private AggregatePolicyRepresentation assertPolicy(AggregatePolicyRepresentation expected, AggregatePolicy policy) {
        AggregatePolicyRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getLogic(), actual.getLogic());

        assertNotNull(actual.getPolicies());
        assertEquals(expected.getPolicies().size(), actual.getPolicies().size());
        assertEquals(0, actual.getPolicies().stream().filter(actualPolicy -> !expected.getPolicies().stream()
                .filter(expectedPolicy -> actualPolicy.equals(expectedPolicy))
                .findFirst().isPresent())
                .count());

        return actual;
    }
}
