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

package org.keycloak.tests.admin.client;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = ClientProtocolValidationTest.OID4VCServerConfig.class)
public class ClientProtocolValidationTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void testCreateClientWithValidProtocol() {
        // Test creating client with valid OIDC protocol
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-oidc-client");
        clientRep.setProtocol("openid-connect");

        String clientId = ApiUtil.getCreatedId(realm.admin().clients().create(clientRep));
        assertNotNull(clientId);

        // Cleanup
        realm.admin().clients().get(clientId).remove();
    }

    @Test
    public void testCreateClientWithSamlProtocol() {
        // Test creating client with valid SAML protocol
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-saml-client");
        clientRep.setProtocol("saml");

        String clientId = ApiUtil.getCreatedId(realm.admin().clients().create(clientRep));
        assertNotNull(clientId);

        // Cleanup
        realm.admin().clients().get(clientId).remove();
    }

    @Test
    public void testCreateClientWithNullProtocol() {
        // Test creating client with null protocol (should default to OIDC)
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-null-protocol-client");
        clientRep.setProtocol(null);

        String clientId = ApiUtil.getCreatedId(realm.admin().clients().create(clientRep));
        assertNotNull(clientId);

        // Verify it defaults to openid-connect
        ClientRepresentation created = realm.admin().clients().get(clientId).toRepresentation();
        assertEquals("openid-connect", created.getProtocol());

        // Cleanup
        realm.admin().clients().get(clientId).remove();
    }

    @Test
    public void testCreateClientWithInvalidProtocol() {
        // Test creating client with non-existent protocol
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-invalid-protocol");
        clientRep.setProtocol("invalid-protocol-xyz");

        Response response = realm.admin().clients().create(clientRep);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        OAuth2ErrorRepresentation error = response.readEntity(OAuth2ErrorRepresentation.class);
        assertNotNull(error);
        assertEquals("Invalid protocol: invalid-protocol-xyz", error.getErrorDescription());
    }

    @Test
    public void testCreateClientWithOid4vcProtocol() {
        // Test that oid4vc protocol is not allowed for clients
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-oid4vc-client");
        clientRep.setProtocol("oid4vc");

        Response response = realm.admin().clients().create(clientRep);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        OAuth2ErrorRepresentation error = response.readEntity(OAuth2ErrorRepresentation.class);
        assertNotNull(error);
        assertEquals("Protocol 'oid4vc' cannot be used as a client protocol", error.getErrorDescription());
    }

    @Test
    public void testUpdateClientWithInvalidProtocol() {
        // Create a valid client first
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-update-protocol");
        clientRep.setProtocol("openid-connect");

        String clientId = ApiUtil.getCreatedId(realm.admin().clients().create(clientRep));
        assertNotNull(clientId);

        // Try to update with invalid protocol
        ClientRepresentation toUpdate = realm.admin().clients().get(clientId).toRepresentation();
        toUpdate.setProtocol("invalid-protocol-abc");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> realm.admin().clients().get(clientId).update(toUpdate));
        assertThat(exception.getResponse().readEntity(String.class), containsString("Invalid protocol"));

        // Cleanup
        realm.admin().clients().get(clientId).remove();
    }

    @Test
    public void testUpdateClientWithOid4vcProtocol() {
        // Create a valid client first
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-update-oid4vc");
        clientRep.setProtocol("openid-connect");

        String clientId = ApiUtil.getCreatedId(realm.admin().clients().create(clientRep));
        assertNotNull(clientId);

        // Try to update with oid4vc protocol
        ClientRepresentation toUpdate = realm.admin().clients().get(clientId).toRepresentation();
        toUpdate.setProtocol("oid4vc");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> realm.admin().clients().get(clientId).update(toUpdate));
        assertThat(exception.getResponse().readEntity(String.class), containsString("cannot be used as a client protocol"));
        realm.admin().clients().get(clientId).remove();
    }

    @Test
    public void testUpdateClientWithValidProtocol() {
        // Create an OIDC client
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-update-valid");
        clientRep.setProtocol("openid-connect");

        String clientId = ApiUtil.getCreatedId(realm.admin().clients().create(clientRep));
        assertNotNull(clientId);

        // Update to SAML protocol - should work
        ClientRepresentation toUpdate = realm.admin().clients().get(clientId).toRepresentation();
        toUpdate.setProtocol("saml");

        realm.admin().clients().get(clientId).update(toUpdate);

        // Verify the protocol was updated
        ClientRepresentation updated = realm.admin().clients().get(clientId).toRepresentation();
        assertEquals("saml", updated.getProtocol());
        // Cleanup
        realm.admin().clients().get(clientId).remove();
    }

    public static class OID4VCServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }
}
