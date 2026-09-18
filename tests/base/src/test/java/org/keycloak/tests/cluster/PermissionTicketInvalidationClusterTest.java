/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.cluster;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.ClusterTestTasks;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

import org.apache.commons.lang3.RandomStringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class PermissionTicketInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<PermissionTicketRepresentation, Object> {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests"}, ref = "cluster-run-on-server")
    RunOnServerClient clusterRunOnServer;

    private String clientId;
    private String userId;
    private String resourceId;
    private String scopeId;
    private final String resourceName = RandomStringUtils.randomAlphabetic(5);
    private final String scopeName = RandomStringUtils.randomAlphabetic(5);

    @Override
    protected void createTestRealm(ContainerInfo node) {
        super.createTestRealm(node);
        createClient(node);
        createUser(node);
        createResource(node);
        createScope(node);
    }

    private void createClient(ContainerInfo node) {
        var client = new ClientRepresentation();
        String s = RandomStringUtils.randomAlphabetic(5);
        client.setClientId("client_" + s);
        client.setName("name_" + s);
        try (var rsp = getAdminClientFor(node)
                .realm(testRealmName)
                .clients()
                .create(client)) {
            clientId = ApiUtil.getCreatedId(rsp);
        }
    }

    private void createUser(ContainerInfo node) {
        var user = createUserRepresentation("user1", "password");
        try (var rsp = getAdminClientFor(node)
                .realm(testRealmName)
                .users()
                .create(user)) {
            userId = ApiUtil.getCreatedId(rsp);
        }
    }

    private UserRepresentation createUserRepresentation(String username, String password) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        return user;
    }

    private void createResource(ContainerInfo node) {
        resourceId = testingClientFor(node).server()
                .fetchString(new ClusterTestTasks.CreateResource(testRealmName, clientId, resourceName))
                .replaceAll("\"", "");
    }

    private void createScope(ContainerInfo node) {
        scopeId = testingClientFor(node).server()
                .fetchString(new ClusterTestTasks.CreateScope(testRealmName, clientId, scopeName))
                .replaceAll("\"", "");
    }

    @Override
    protected PermissionTicketRepresentation createTestEntityRepresentation() {
        var ticket = new PermissionTicketRepresentation();
        ticket.setGranted(true);
        ticket.setOwner(clientId); // client is the owner
        ticket.setRequester(userId); // userid is the requester
        ticket.setScope(scopeId);
        ticket.setResource(resourceId);
        return ticket;
    }

    @Override
    protected Object entityResource(PermissionTicketRepresentation testEntity, ContainerInfo node) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object entityResource(String idOrName, ContainerInfo node) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected PermissionTicketRepresentation createEntity(PermissionTicketRepresentation testEntity, ContainerInfo node) {
        return testingClientFor(node).server().fetch(
                new ClusterTestTasks.CreatePermissionTicket(testRealmName, clientId, scopeId, resourceId, userId),
                PermissionTicketRepresentation.class);
    }

    @Override
    protected PermissionTicketRepresentation readEntity(PermissionTicketRepresentation entity, ContainerInfo node) {
        return testingClientFor(node).server().fetch(
                new ClusterTestTasks.ReadPermissionTicket(testRealmName, clientId, entity.getId()),
                PermissionTicketRepresentation.class);
    }

    @Override
    protected PermissionTicketRepresentation updateEntity(PermissionTicketRepresentation entity, ContainerInfo node) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void deleteEntity(PermissionTicketRepresentation entity, ContainerInfo node) {
        testingClientFor(node).server().run(new ClusterTestTasks.DeletePermissionTicket(testRealmName, entity.getId()));
    }

    @Override
    protected PermissionTicketRepresentation testEntityUpdates(PermissionTicketRepresentation entity, boolean backendFailover) {
        final long timestamp = ThreadLocalRandom.current().nextLong(100000);
        testingClientFor(getCurrentFailNode()).server().run(
                new ClusterTestTasks.UpdatePermissionTicketTimestamp(testRealmName, clientId, entity.getId(), timestamp));

        if (backendFailover) {
            failure();
        }

        for (var node : getCurrentSurvivorNodes()) {
            var rsp = testingClientFor(node).server()
                    .fetchString(new ClusterTestTasks.ReadPermissionTicketTimestamp(testRealmName, clientId, entity.getId()));
            assertEquals(timestamp, Long.parseLong(rsp.replaceAll("\"", "")));
        }

        failback();
        iterateCurrentFailNode();

        return entity;
    }

    private ClusterTestingClient testingClientFor(ContainerInfo node) {
        return new ClusterTestingClient(node.getIndex(), loadBalancer, clusterRunOnServer);
    }
}
