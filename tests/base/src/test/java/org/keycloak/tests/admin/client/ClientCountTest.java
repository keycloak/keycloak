/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.client;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@code GET /admin/realms/{realm}/clients/count}.
 */
@KeycloakIntegrationTest(config = ClientCountTest.SearchableServer.class)
public class ClientCountTest {

    @InjectRealm
    ManagedRealm testRealm;

    private static final String ATTR_KEY = "env";
    private static final String ATTR_VAL = "production";

    private List<String> createdIds;

    @BeforeEach
    public void createClients() {
        ClientsResource clients = testRealm.admin().clients();

        ClientRepresentation alpha = new ClientRepresentation();
        alpha.setClientId("count-test-alpha");
        alpha.setEnabled(true);

        ClientRepresentation beta = new ClientRepresentation();
        beta.setClientId("count-test-beta");
        beta.setEnabled(true);
        beta.setAttributes(java.util.Map.of(ATTR_KEY, ATTR_VAL));

        ClientRepresentation gamma = new ClientRepresentation();
        gamma.setClientId("count-test-gamma");
        gamma.setEnabled(true);
        gamma.setAttributes(java.util.Map.of(ATTR_KEY, ATTR_VAL));

        try (Response r1 = clients.create(alpha);
             Response r2 = clients.create(beta);
             Response r3 = clients.create(gamma)) {
            createdIds = List.of(
                    r1.getLocation().getPath().replaceAll(".*/", ""),
                    r2.getLocation().getPath().replaceAll(".*/", ""),
                    r3.getLocation().getPath().replaceAll(".*/", ""));
        }
    }

    @AfterEach
    public void deleteClients() {
        if (createdIds != null) {
            for (String id : createdIds) {
                try {
                    testRealm.admin().clients().get(id).remove();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    public void testTotalCountIncludesCreatedClients() {
        Long total = testRealm.admin().clients().count();
        assertTrue(total >= 3, "Expected at least 3 clients, got " + total);
    }

    @Test
    public void testCountWithSearchFilter() {
        Long count = testRealm.admin().clients().count("count-test", null);
        assertEquals(3L, count, "Expected 3 clients matching 'count-test'");
    }

    @Test
    public void testCountWithAttributeFilter() {
        Long count = testRealm.admin().clients().count(null, ATTR_KEY + ":" + ATTR_VAL);
        assertEquals(2L, count, "Expected 2 clients with attribute " + ATTR_KEY + "=" + ATTR_VAL);
    }

    @Test
    public void testCountDecreasesAfterDeletion() {
        Long before = testRealm.admin().clients().count("count-test", null);
        testRealm.admin().clients().get(createdIds.get(0)).remove();
        createdIds = createdIds.subList(1, createdIds.size());
        Long after = testRealm.admin().clients().count("count-test", null);
        assertEquals(before - 1, after, "Count should decrease by 1 after deletion");
    }

    @Test
    public void testSearchFilterNoMatch() {
        Long count = testRealm.admin().clients().count("no-such-client-xyz", null);
        assertEquals(0L, count, "Expected 0 clients for non-matching search");
    }

    public static class SearchableServer implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-client-jpa-searchable-attributes", ATTR_KEY);
        }
    }
}
