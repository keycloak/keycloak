/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.authz.admin;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resource URI templates that the resource endpoints reject, such as a wildcard in the middle of a path, must be
 * rejected consistently on every path that creates or imports resources.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/55592">#55592</a>
 */
@KeycloakIntegrationTest
public class ResourceServerUriTemplateValidationTest {

    private static final String INVALID_URI = "/api/*/info";

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(config = ResourceServerConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient resourceServer;

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    public void testImportSettingsRejectsInvalidResourceUri() {
        ResourceServerRepresentation settings = new ResourceServerRepresentation();
        ResourceRepresentation resource = new ResourceRepresentation("invalid-uri-resource");

        resource.setUris(Set.of(INVALID_URI));
        settings.setResources(List.of(resource));

        assertThrows(BadRequestException.class, () -> resourceServer.admin().authorization().importSettings(settings));

        assertTrue(resourceServer.admin().authorization().exportSettings().getResources().isEmpty());
    }

    @Test
    public void testClientCreateWithAuthorizationSettingsRejectsInvalidResourceUri() {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId("invalid-uri-client-create-test");
        client.setServiceAccountsEnabled(true);
        client.setAuthorizationServicesEnabled(true);

        ResourceServerRepresentation authzSettings = new ResourceServerRepresentation();
        ResourceRepresentation resource = new ResourceRepresentation("invalid-uri-resource");

        resource.setUris(Set.of(INVALID_URI));
        authzSettings.setResources(List.of(resource));
        client.setAuthorizationSettings(authzSettings);

        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(400, response.getStatus());
            assertThat(response.readEntity(String.class), containsString("is not a valid template"));
        }

        assertTrue(realm.admin().clients().findByClientId(client.getClientId()).isEmpty());
    }

    @Test
    public void testRealmImportRejectsInvalidResourceUri() {
        String realmName = "invalid-uri-realm-import-test";
        RealmRepresentation realmRep = new RealmRepresentation();

        realmRep.setRealm(realmName);
        realmRep.setEnabled(true);

        ClientRepresentation client = new ClientRepresentation();

        client.setClientId("invalid-uri-client");
        client.setServiceAccountsEnabled(true);
        client.setAuthorizationServicesEnabled(true);

        ResourceServerRepresentation authzSettings = new ResourceServerRepresentation();
        ResourceRepresentation resource = new ResourceRepresentation("invalid-uri-resource");

        resource.setUris(Set.of(INVALID_URI));
        authzSettings.setResources(List.of(resource));
        client.setAuthorizationSettings(authzSettings);

        realmRep.setClients(List.of(client));

        try {
            assertThrows(BadRequestException.class, () -> adminClient.realms().create(realmRep));
            assertThrows(NotFoundException.class, () -> adminClient.realm(realmName).toRepresentation());
        } finally {
            try {
                adminClient.realm(realmName).remove();
            } catch (NotFoundException ignored) {
            }
        }
    }

    public static class ResourceServerConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("resource-server")
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .directAccessGrantsEnabled();
        }
    }
}
