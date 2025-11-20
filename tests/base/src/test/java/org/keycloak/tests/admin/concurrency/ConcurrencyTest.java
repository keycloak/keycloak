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

package org.keycloak.tests.admin.concurrency;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class ConcurrencyTest extends AbstractConcurrencyTest {

    // Verify that no attribute values are duplicated, and there are no locking exceptions when adding attributes in parallel
    // https://github.com/keycloak/keycloak/issues/38868
    @Test
    @Disabled
    public void createUserAttributes() throws Throwable {
        AtomicInteger c = new AtomicInteger();

        UsersResource users = managedRealm.admin().users();

        // enable unmanaged attributes
        UserProfileUtil.enableUnmanagedAttributes(users.userProfile());

        UserRepresentation u = UserConfigBuilder.create().username("attributes").build();

        String userId;
        try (Response response = users.create(u)) {
            userId = ApiUtil.getCreatedId(response);
        }

        UserResource user = users.get(userId);

        concurrentTest((threadIndex, keycloak, realm) -> {
            UserRepresentation rep = user.toRepresentation();
            rep.singleAttribute("a-" + c.getAndIncrement(), "value");
            user.update(rep);
        });

        UserRepresentation rep = user.toRepresentation();

        // Number of attributes should be equal to created attributes, or less (concurrent requests may drop attributes added by other threads)
        assertTrue(rep.getAttributes().size() <= c.get());
        // All attributes should have a single value
        for (Map.Entry<String, List<String>> e : rep.getAttributes().entrySet()) {
            assertEquals(1, e.getValue().size());
        }
    }

    @Test
    public void testAllConcurrently() throws Throwable {
        AtomicInteger uniqueCounter = new AtomicInteger(100000);
        concurrentTest(
          new CreateClient(uniqueCounter),
          new CreateRemoveClient(uniqueCounter),
          new CreateGroup(uniqueCounter),
          new CreateRole(uniqueCounter)
        );
    }

    @Test
    public void createClient() throws Throwable {
        AtomicInteger uniqueCounter = new AtomicInteger();
        concurrentTest(new CreateClient(uniqueCounter));
    }

    @Test
    public void createGroup() throws Throwable {
        AtomicInteger uniqueCounter = new AtomicInteger();
        concurrentTest(new CreateGroup(uniqueCounter));
    }

    @Test
    public void createRemoveClient() throws Throwable {
        // FYI< this will fail as HSQL seems to be trying to perform table locks.
        AtomicInteger uniqueCounter = new AtomicInteger();
        concurrentTest(new CreateRemoveClient(uniqueCounter));
    }

    @Test
    public void createClientRole() throws Throwable {
        ClientRepresentation c = new ClientRepresentation();
        c.setClientId("client");
        final String clientId;
        try (Response response = managedRealm.admin().clients().create(c)) {
            clientId = ApiUtil.getCreatedId(response);
        }

        AtomicInteger uniqueCounter = new AtomicInteger();
        concurrentTest(new CreateClientRole(uniqueCounter, clientId));
    }

    @Test
    public void createRole() throws Throwable {
        AtomicInteger uniqueCounter = new AtomicInteger();
        run(new CreateRole(uniqueCounter));
    }

    public void concurrentTest(KeycloakRunnable... tasks) throws Throwable {
        System.out.println("***************************");
        long start = System.currentTimeMillis();
        run(tasks);
        long end = System.currentTimeMillis() - start;
        System.out.println("took " + end + " ms");
    }

    private class CreateClient implements KeycloakRunnable {

        private final AtomicInteger clientIndex;

        public CreateClient(AtomicInteger clientIndex) {
            this.clientIndex = clientIndex;
        }

        @Override
        public void run(int threadIndex, Keycloak keycloak, RealmResource realm) throws Throwable {
            String name = "cc-" + clientIndex.getAndIncrement();
            ClientRepresentation c = new ClientRepresentation();
            c.setClientId(name);
            String id;
            try (Response response = realm.clients().create(c)) {
                id = ApiUtil.getCreatedId(response);
            }

            c = realm.clients().get(id).toRepresentation();
            assertNotNull(c);

            int findAttempts = 5;

            boolean clientFound = IntStream.range(0, findAttempts)
                    .anyMatch(i -> realm.clients().findAll().stream()
                            .map(ClientRepresentation::getClientId)
                            .filter(Objects::nonNull)
                            .anyMatch(name::equals));

            assertTrue(clientFound, "Client " + name + " not found in client list after " + findAttempts + " attempts");
        }
    }

    private class CreateRemoveClient implements KeycloakRunnable {

        private final AtomicInteger clientIndex;

        public CreateRemoveClient(AtomicInteger clientIndex) {
            this.clientIndex = clientIndex;
        }

        @Override
        public void run(int threadIndex, Keycloak keycloak, RealmResource realm) throws Throwable {
            String name = "crc-" + clientIndex.getAndIncrement();
            ClientRepresentation c = new ClientRepresentation();
            c.setClientId(name);
            final ClientsResource clients = realm.clients();

            String id;
            try (Response response = clients.create(c)) {
                id = ApiUtil.getCreatedId(response);
            }
            final ClientResource client = clients.get(id);

            c = client.toRepresentation();
            assertNotNull(c);

            int findAttempts = 5;

            boolean clientFound = IntStream.range(0, findAttempts)
                    .anyMatch(i -> clients.findAll().stream()
                    .map(ClientRepresentation::getClientId)
                    .filter(Objects::nonNull)
                    .anyMatch(name::equals));

            assertTrue(clientFound, "Client " + name + " not found in client list after " + findAttempts + " attempts");

            client.remove();
            try {
                client.toRepresentation();
                fail("Client " + name + " should not be found.  Should throw a 404");
            } catch (NotFoundException e) {

            }

            assertFalse(clients.findAll().stream()
                .map(ClientRepresentation::getClientId)
                .filter(Objects::nonNull)
                .anyMatch(name::equals), "Client " + name + " should now not present in client list");
        }
    }

    private class CreateGroup implements KeycloakRunnable {

        private final AtomicInteger uniqueIndex;

        public CreateGroup(AtomicInteger uniqueIndex) {
            this.uniqueIndex = uniqueIndex;
        }

        @Override
        public void run(int threadIndex, Keycloak keycloak, RealmResource realm) throws Throwable {
            String name = "g-" + uniqueIndex.getAndIncrement();
            GroupRepresentation c = new GroupRepresentation();
            c.setName(name);
            String id;
            try (Response response = realm.groups().add(c)) {
                id = ApiUtil.getCreatedId(response);
            }

            c = realm.groups().group(id).toRepresentation();
            assertNotNull(c);

            assertTrue(realm.groups().groups().stream()
                .map(GroupRepresentation::getName)
                .filter(Objects::nonNull)
                .anyMatch(name::equals), "Group " + name + " [" + id + "] " + " not found in group list");
        }
    }

    private class CreateClientRole implements KeycloakRunnable {

        private final AtomicInteger uniqueCounter;
        private final String clientId;

        public CreateClientRole(AtomicInteger uniqueCounter, String clientId) {
            this.uniqueCounter = uniqueCounter;
            this.clientId = clientId;
        }

        @Override
        public void run(int threadIndex, Keycloak keycloak, RealmResource realm) throws Throwable {
            String name = "cr-" + uniqueCounter.getAndIncrement();
            RoleRepresentation r = new RoleRepresentation(name, null, false);

            final RolesResource roles = realm.clients().get(clientId).roles();
            roles.create(r);
            assertNotNull(roles.get(name).toRepresentation());
        }
    }

    private class CreateRole implements KeycloakRunnable {

        private final AtomicInteger uniqueCounter;

        public CreateRole(AtomicInteger uniqueCounter) {
            this.uniqueCounter = uniqueCounter;
        }

        @Override
        public void run(int threadIndex, Keycloak keycloak, RealmResource realm) throws Throwable {
            String name = "r-" + uniqueCounter.getAndIncrement();
            RoleRepresentation r = new RoleRepresentation(name, null, false);

            final RolesResource roles = realm.roles();
            roles.create(r);
            assertNotNull(roles.get(name).toRepresentation());
        }
    }
}
