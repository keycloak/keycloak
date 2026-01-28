/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.authz;

import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse.PolicyResultRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import org.junit.Assert;
import org.junit.Test;

public class UmaRepresentationTest extends AbstractResourceServerTest {
    private ResourceRepresentation resource;
    private PermissionResource permission;

    private void createPermissionTicket() {
        PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();
        ticket.setOwner(resource.getOwner().getId());
        ticket.setResource(resource.getId());
        ticket.setRequesterName("kolo");
        ticket.setScopeName("ScopeA");
        ticket.setGranted(true);
        permission.create(ticket);
    }

    @Test
    public void testCanRepresentPermissionTicketWithNamesOfResourceOwnedByUser() throws Exception {
        resource = addResource("Resource A", "marta", true, "ScopeA");
        permission = getAuthzClient().protection("marta", "password").permission();
        createPermissionTicket();

        List<PermissionTicketRepresentation> permissionTickets = permission.find(resource.getId(), null, null, null, null, true, null, null);
        Assert.assertFalse(permissionTickets.isEmpty());
        Assert.assertEquals(1, permissionTickets.size());

        PermissionTicketRepresentation ticket = permissionTickets.get(0);
        Assert.assertEquals(ticket.getOwnerName(), "marta");
        Assert.assertEquals(ticket.getRequesterName(), "kolo");
        Assert.assertEquals(ticket.getResourceName(), "Resource A");
        Assert.assertEquals(ticket.getScopeName(), "ScopeA");
        Assert.assertTrue(ticket.isGranted());
    }

    @Test
    public void testCanRepresentPermissionTicketWithNamesOfResourceOwnedByClient() throws Exception {
        resource = addResource("Resource A", getClient(getRealm()).toRepresentation().getId(), true, "ScopeA");
        permission = getAuthzClient().protection().permission();
        createPermissionTicket();

        List<PermissionTicketRepresentation> permissionTickets = permission.find(resource.getId(), null, null, null, null, true, null, null);
        Assert.assertFalse(permissionTickets.isEmpty());
        Assert.assertEquals(1, permissionTickets.size());

        PermissionTicketRepresentation ticket = permissionTickets.get(0);
        Assert.assertEquals(ticket.getOwnerName(), "resource-server-test");
        Assert.assertEquals(ticket.getRequesterName(), "kolo");
        Assert.assertEquals(ticket.getResourceName(), "Resource A");
        Assert.assertEquals(ticket.getScopeName(), "ScopeA");
        Assert.assertTrue(ticket.isGranted());
    }

    @Test
    public void testCanRepresentPolicyResultGrantOfResourceOwnedByUser() throws Exception {
        resource = addResource("Resource A", "marta", true, "ScopeA");
        permission = getAuthzClient().protection("marta", "password").permission();
        createPermissionTicket();

        RealmResource realm = getRealm();
        String resourceServerId = getClient(realm).toRepresentation().getId();
        UserRepresentation user = realm.users().search("kolo").get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(resourceServerId);
        request.addResource("Resource A", "ScopeA");
        PolicyEvaluationResponse result = getClient(realm).authorization().policies().evaluate(request);
        Assert.assertEquals(result.getStatus(), DecisionEffect.PERMIT);

        List<PolicyEvaluationResponse.EvaluationResultRepresentation> evaluations = result.getResults();
        Assert.assertFalse(evaluations.isEmpty());
        Assert.assertEquals(1, evaluations.size());

        Set<PolicyResultRepresentation> policies = evaluations.get(0).getPolicies();
        Assert.assertFalse(evaluations.isEmpty());
        Assert.assertEquals(1, evaluations.size());

        String description = policies.iterator().next().getPolicy().getDescription();
        Assert.assertTrue(description.startsWith("Resource owner (marta) grants access"));
    }

    @Test
    public void testCanRepresentPolicyResultGrantOfResourceOwnedByClient() throws Exception {
        resource = addResource("Resource A", getClient(getRealm()).toRepresentation().getId(), true, "ScopeA");
        permission = getAuthzClient().protection().permission();
        createPermissionTicket();

        RealmResource realm = getRealm();
        String resourceServerId = getClient(realm).toRepresentation().getId();
        UserRepresentation user = realm.users().search("kolo").get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(resourceServerId);
        request.addResource("Resource A", "ScopeA");
        PolicyEvaluationResponse result = getClient(realm).authorization().policies().evaluate(request);
        Assert.assertEquals(result.getStatus(), DecisionEffect.PERMIT);

        List<PolicyEvaluationResponse.EvaluationResultRepresentation> evaluations = result.getResults();
        Assert.assertFalse(evaluations.isEmpty());
        Assert.assertEquals(1, evaluations.size());

        Set<PolicyEvaluationResponse.PolicyResultRepresentation> policies = evaluations.get(0).getPolicies();
        Assert.assertFalse(evaluations.isEmpty());
        Assert.assertEquals(1, evaluations.size());

        String description = policies.iterator().next().getPolicy().getDescription();
        Assert.assertTrue(description.startsWith("Resource owner (resource-server-test) grants access"));
    }
}
