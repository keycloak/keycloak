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
 *
 */

package org.keycloak.tests.oid4vc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.keycloak.VCFormat.JWT_VC;
import static org.keycloak.VCFormat.SD_JWT_VC;
import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;
import static org.keycloak.models.ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE;
import static org.keycloak.models.oid4vci.CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VCT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BINDING_REQUIRED;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BINDING_REQUIRED_PROOF_TYPES;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_HASH_ALGORITHM;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_HASH_ALGORITHM_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_JWT_VC;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_SD_JWT_VC;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONFIGURATION_ID;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONTEXTS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_EXPIRY_IN_SECONDS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_EXPIRY_IN_SECONDS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_FORMAT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_FORMAT_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_INCLUDE_IN_METADATA;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SD_JWT_NUMBER_OF_DECOYS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SD_JWT_NUMBER_OF_DECOYS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SUPPORTED_TYPES;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pascal Knüppel
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCClientScopeTest extends OID4VCIssuerTestBase {

    @DisplayName("Verify default values are correctly set")
    @Test
    public void testCredentialScopeDefaultAttributes() {

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol(OID4VC_PROTOCOL);
        clientScope.setAttributes(Map.of("test-attribute", "test-value"));

        String clientScopeId = null;
        ClientScopesResource clientScopes = testRealm.admin().clientScopes();
        try (Response response = clientScopes.create(clientScope)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientScopeId = ApiUtil.getCreatedId(response);

            clientScope = clientScopes.get(clientScopeId).toRepresentation();
            assertNotNull(clientScope);

            Map<String, String> attrs = new HashMap<>(clientScope.getAttributes());
            assertEquals("test-value", attrs.remove("test-attribute"));
            assertEquals("true", attrs.remove(INCLUDE_IN_TOKEN_SCOPE));

            assertEquals(VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS_DEFAULT, attrs.remove(VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS));
            assertEquals(String.valueOf(VC_SD_JWT_NUMBER_OF_DECOYS_DEFAULT), attrs.remove(VC_SD_JWT_NUMBER_OF_DECOYS));
            assertEquals(VC_FORMAT_DEFAULT, attrs.remove(VC_FORMAT));
            assertEquals(VC_BUILD_CONFIG_HASH_ALGORITHM_DEFAULT, attrs.remove(VC_BUILD_CONFIG_HASH_ALGORITHM));
            assertEquals(VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_SD_JWT_VC, attrs.remove(VC_BUILD_CONFIG_TOKEN_JWS_TYPE));
            assertEquals(String.valueOf(VC_EXPIRY_IN_SECONDS_DEFAULT), attrs.remove(VC_EXPIRY_IN_SECONDS));
            assertEquals(CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT, attrs.remove(VC_CRYPTOGRAPHIC_BINDING_METHODS));
            assertEquals(clientScope.getName(), attrs.remove(VC_CONFIGURATION_ID));
            assertEquals(clientScope.getName(), attrs.remove(VC_SUPPORTED_TYPES));
            assertEquals(clientScope.getName(), attrs.remove(VC_CONTEXTS));
            assertEquals(clientScope.getName(), attrs.remove(VCT));
            assertEquals("true", attrs.remove(VC_INCLUDE_IN_METADATA));

            assertEquals(Set.of(), attrs.keySet(), "Untested attributes");

            // Note: ISSUER_DID is intentionally not set by default, as there's no sensible default
            // The implementation leaves it undefined so the realm's URL will be used as the Issuer's ID

        } finally {
            assertNotNull(clientScopeId);
            clientScopes.get(clientScopeId).remove();
        }
    }

    @DisplayName("Verify CRUD of clientScope when OID4VCI is disabled for the realm")
    @Test
    public void testCreateCredentialScopeForDisabledRealm() {
        RealmRepresentation realm = testRealm.admin().toRepresentation();
        ClientScopesResource clientScopes = testRealm.admin().clientScopes();
        try {
            // Create clientScope1 successfully
            String clientScopeId;
            ClientScopeRepresentation clientScopeRep = new ClientScopeRepresentation();
            clientScopeRep.setName("test-client-scope1");
            clientScopeRep.setDescription("test-client-scope-description");
            clientScopeRep.setProtocol(OID4VC_PROTOCOL);
            clientScopeRep.setAttributes(Map.of("test-attribute", "test-value"));
            try (Response response = clientScopes.create(clientScopeRep)) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
                clientScopeId = ApiUtil.getCreatedId(response);
            }

            // Disable OID4VCI for the realm
            realm.setVerifiableCredentialsEnabled(false);
            testRealm.admin().update(realm);

            // Test not possible to update existing oid4vci client-scope
            ClientScopeResource clientScopeRes = clientScopes.get(clientScopeId);
            clientScopeRep = clientScopeRes.toRepresentation();
            clientScopeRep.setDescription("Foo");
            try {
                clientScopeRes.update(clientScopeRep);
                Assert.fail("Not expected to update client scope");
            } catch (BadRequestException bre) {
                // expected
            }

            // Still possible to delete oid4vci clientScope
            clientScopeRes.remove();

            // Not possible to create new oid4vci clientScope
            clientScopeRep = new ClientScopeRepresentation();
            clientScopeRep.setName("test-client-scope2");
            clientScopeRep.setDescription("test-client-scope-description");
            clientScopeRep.setProtocol(OID4VC_PROTOCOL);
            clientScopeRep.setAttributes(Map.of("test-attribute", "test-value"));
            try (Response response = clientScopes.create(clientScopeRep)) {
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            }
        } finally {
            // Revert
            realm.setVerifiableCredentialsEnabled(true);
            testRealm.admin().update(realm);
        }
    }

    @Test
    public void testCreateRealmWithDefaultClientScopes() {
        RealmsResource realms = keycloak.realms();
        String realmName = "aux-oid4vci-realm";
        try {
            RealmRepresentation realmRep = new RealmRepresentation();
            realmRep.setRealm(realmName);
            realmRep.setVerifiableCredentialsEnabled(true);
            realms.create(realmRep);

            RealmResource realm = realms.realm(realmName);
            realmRep = realm.toRepresentation();
            assertTrue(realmRep.isVerifiableCredentialsEnabled());

            List<CredentialScopeRepresentation> clientScopes = realm.clientScopes().findAll().stream()
                    .filter(cs -> OID4VC_PROTOCOL.equals(cs.getProtocol()))
                    .map(CredentialScopeRepresentation::new)
                    .toList();

            // expected: natural_person_jwt, natural_person_sd
            assertEquals(2, clientScopes.size());

            for (CredentialScopeRepresentation cs : clientScopes) {
                Map<String, String> attrs = new HashMap<>(cs.getAttributes());
                assertEquals("true", attrs.remove(INCLUDE_IN_TOKEN_SCOPE));
                assertEquals("true", attrs.remove(VC_INCLUDE_IN_METADATA));
                assertEquals("true", attrs.remove(VC_BINDING_REQUIRED));

                assertEquals(VC_BUILD_CONFIG_HASH_ALGORITHM_DEFAULT, attrs.remove(VC_BUILD_CONFIG_HASH_ALGORITHM));
                assertEquals(String.valueOf(VC_EXPIRY_IN_SECONDS_DEFAULT), attrs.remove(VC_EXPIRY_IN_SECONDS));
                assertEquals(CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT, attrs.remove(VC_CRYPTOGRAPHIC_BINDING_METHODS));
                assertEquals("jwt,attestation", attrs.remove(VC_BINDING_REQUIRED_PROOF_TYPES));
                assertEquals("oid4vc_natural_person", attrs.remove(VC_SUPPORTED_TYPES));
                assertEquals("oid4vc_natural_person", attrs.remove(VC_CONTEXTS));
                assertEquals("oid4vc_natural_person", attrs.remove(VCT));

                switch (attrs.remove(VC_FORMAT)) {
                    case JWT_VC: {
                        assertEquals("oid4vc_natural_person_jwt", attrs.remove(VC_CONFIGURATION_ID));
                        assertEquals(VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_JWT_VC, attrs.remove(VC_BUILD_CONFIG_TOKEN_JWS_TYPE));
                        break;
                    }
                    case SD_JWT_VC: {
                        assertEquals("oid4vc_natural_person_sd", attrs.remove(VC_CONFIGURATION_ID));
                        assertEquals(VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_SD_JWT_VC, attrs.remove(VC_BUILD_CONFIG_TOKEN_JWS_TYPE));
                        assertEquals(VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS_DEFAULT, attrs.remove(VC_BUILD_CONFIG_SD_JWT_VISIBLE_CLAIMS));
                        assertEquals(String.valueOf(VC_SD_JWT_NUMBER_OF_DECOYS_DEFAULT), attrs.remove(VC_SD_JWT_NUMBER_OF_DECOYS));
                        break;
                    }
                }

                assertEquals(Set.of(), attrs.keySet(), "Untested attributes");
            }
        } finally {
            realms.realm(realmName).remove();
        }
    }
}
