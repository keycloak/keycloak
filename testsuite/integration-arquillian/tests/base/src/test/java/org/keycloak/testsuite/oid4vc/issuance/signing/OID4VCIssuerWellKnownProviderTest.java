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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OID4VCIssuerWellKnownProviderTest extends OID4VCTest {

    @Test
    public void getConfig() {
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
        String expectedCredentialsEndpoint = expectedIssuer + "/protocol/oid4vc/credential";
        String expectedAuthorizationServer = expectedIssuer;
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    Object issuerConfig = oid4VCIssuerWellKnownProvider.getConfig();
                    assertTrue("Valid credential-issuer metadata should be returned.", issuerConfig instanceof CredentialIssuer);
                    CredentialIssuer credentialIssuer = (CredentialIssuer) issuerConfig;
                    assertEquals("The correct issuer should be included.", expectedIssuer, credentialIssuer.getCredentialIssuer());
                    assertEquals("The correct credentials endpoint should be included.", expectedCredentialsEndpoint, credentialIssuer.getCredentialEndpoint());
                    assertEquals("Since the authorization server is equal to the issuer, just 1 should be returned.", 1, credentialIssuer.getAuthorizationServers().size());
                    assertEquals("The expected server should have been returned.", expectedAuthorizationServer, credentialIssuer.getAuthorizationServers().get(0));
                    assertTrue("The test-credential should be supported.", credentialIssuer.getCredentialsSupported().containsKey("test-credential"));
                    assertEquals("The test-credential should offer type VerifiableCredential", "VerifiableCredential", credentialIssuer.getCredentialsSupported().get("test-credential").getScope());
                    assertEquals("The test-credential should be offered in the jwt-vc format.", Format.JWT_VC, credentialIssuer.getCredentialsSupported().get("test-credential").getFormat());
                }));
    }


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() != null) {
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(RSA_KEY));
            testRealm.getComponents().add("org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService", getJwtSigningProvider(RSA_KEY));
        } else {
            testRealm.setComponents(new MultivaluedHashMap<>(
                    Map.of("org.keycloak.keys.KeyProvider", List.of(getRsaKeyProvider(RSA_KEY)),
                            "org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService", List.of(getJwtSigningProvider(RSA_KEY))
                    )));
        }
        ClientRepresentation clientRepresentation = getTestClient("did:web:test.org");
        if (testRealm.getClients() != null) {
            testRealm.getClients().add(clientRepresentation);
        } else {
            testRealm.setClients(List.of(clientRepresentation));
        }
        if (testRealm.getUsers() != null) {
            testRealm.getUsers().add(getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole"))));
        } else {
            testRealm.setUsers(List.of(getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole")))));
        }
        if (testRealm.getAttributes() != null) {
            testRealm.getAttributes().put("issuerDid", TEST_DID.toString());
        } else {
            testRealm.setAttributes(Map.of("issuerDid", TEST_DID.toString()));
        }
    }
}
