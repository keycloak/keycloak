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
package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientPoliciesResource;
import org.keycloak.admin.client.resource.ClientPolicyResource;
import org.keycloak.admin.client.resource.PolicyResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ClientPolicyManagementTest extends AbstractPolicyManagementTest {

    @Override
    protected RealmBuilder createTestRealm() {
        return super.createTestRealm()
                .client(ClientBuilder.create().clientId("Client A"))
                .client(ClientBuilder.create().clientId("Client B"))
                .client(ClientBuilder.create().clientId("Client C"));
    }

    @Test
    public void testCreate() {
        AuthorizationResource authorization = getClient().authorization();
        ClientPolicyRepresentation representation = new ClientPolicyRepresentation();

        representation.setName("Realm Client Policy");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addClient("Client A");
        representation.addClient("Client B");

        assertCreated(authorization, representation);
    }

    @Test
    public void testUpdate() {
        AuthorizationResource authorization = getClient().authorization();
        ClientPolicyRepresentation representation = new ClientPolicyRepresentation();

        representation.setName("Update Test Client Policy");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addClient("Client A");
        representation.addClient("Client B");
        representation.addClient("Client C");

        assertCreated(authorization, representation);

        representation.setName("changed");
        representation.setDescription("changed");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.setClients(representation.getClients().stream().filter(userName -> !userName.equals("Client A")).collect(Collectors.toSet()));

        ClientPoliciesResource policies = authorization.policies().client();
        ClientPolicyResource permission = policies.findById(representation.getId());

        permission.update(representation);
        assertRepresentation(representation, permission);

        representation.setClients(representation.getClients().stream().filter(userName -> !userName.equals("Client C")).collect(Collectors.toSet()));

        permission.update(representation);
        assertRepresentation(representation, permission);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient().authorization();
        ClientPolicyRepresentation representation = new ClientPolicyRepresentation();

        representation.setName("Test Delete Permission");
        representation.addClient("Client A");

        ClientPoliciesResource policies = authorization.policies().client();
        Response response = policies.create(representation);
        ClientPolicyRepresentation created = response.readEntity(ClientPolicyRepresentation.class);
        response.close();

        policies.findById(created.getId()).remove();

        ClientPolicyResource removed = policies.findById(created.getId());

        try {
            removed.toRepresentation();
            fail("Permission not removed");
        } catch (NotFoundException ignore) {

        }
    }

    @Test
    public void testGenericConfig() {
        AuthorizationResource authorization = getClient().authorization();
        ClientPolicyRepresentation representation = new ClientPolicyRepresentation();

        representation.setName("Test Generic Config Permission");
        representation.addClient("Client A");

        ClientPoliciesResource policies = authorization.policies().client();
        Response response = policies.create(representation);
        ClientPolicyRepresentation created = response.readEntity(ClientPolicyRepresentation.class);
        response.close();

        PolicyResource policy = authorization.policies().policy(created.getId());
        PolicyRepresentation genericConfig = policy.toRepresentation();

        assertNotNull(genericConfig.getConfig());
        assertNotNull(genericConfig.getConfig().get("clients"));

        ClientRepresentation user = getRealm().clients().findByClientId("Client A").get(0);

        assertTrue(genericConfig.getConfig().get("clients").contains(user.getId()));
    }

    private void assertCreated(AuthorizationResource authorization, ClientPolicyRepresentation representation) {
        ClientPoliciesResource permissions = authorization.policies().client();
        Response response = permissions.create(representation);
        ClientPolicyRepresentation created = response.readEntity(ClientPolicyRepresentation.class);
        response.close();
        ClientPolicyResource permission = permissions.findById(created.getId());
        assertRepresentation(representation, permission);
    }

    private void assertRepresentation(ClientPolicyRepresentation representation, ClientPolicyResource permission) {
        ClientPolicyRepresentation actual = permission.toRepresentation();
        assertRepresentation(representation, actual, () -> permission.resources(), () -> Collections.emptyList(), () -> permission.associatedPolicies());
        assertEquals(representation.getClients().size(), actual.getClients().size());
        assertEquals(0, actual.getClients().stream().filter(clientId -> !representation.getClients().stream()
                .filter(userName -> getClientName(clientId).equalsIgnoreCase(userName))
                .findFirst().isPresent())
                .count());
    }

    private String getClientName(String id) {
        return getRealm().clients().get(id).toRepresentation().getClientId();
    }
}
