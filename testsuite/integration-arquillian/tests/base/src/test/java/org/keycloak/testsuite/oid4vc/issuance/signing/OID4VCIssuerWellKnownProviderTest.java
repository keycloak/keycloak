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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.client.KeycloakTestingClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.AbstractTestRealmKeycloakTest.TEST_REALM_NAME;
import static org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCTest.RSA_KEY;
import static org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCTest.TEST_DID;
import static org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCTest.getCredentialBuilderProvider;
import static org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCTest.getRsaKeyProvider;

@RunWith(Enclosed.class)
public class OID4VCIssuerWellKnownProviderTest {

    public static class TestAttributesOverride extends OID4VCTest {
        @Test
        public void testRealmAttributesOverrideClientAttributes() {
            OID4VCIssuerWellKnownProviderTest
                    .testCredentialConfig(suiteContext, testingClient);
        }

        @Override
        public void configureTestRealm(RealmRepresentation testRealm) {
            ClientRepresentation testClient = getTestClient("did:web:test.org");
            Map<String, String> clientAttributes = new HashMap<>(getTestCredentialDefinitionAttributes());
            Map<String, String> realmAttributes = new HashMap<>();
            // We'll change the client attributes and put the correct value in the realm
            // attributes and expect the test to work.
            clientAttributes.put("vc.test-credential.expiry_in_s", "20");
            realmAttributes.put("vc.test-credential.expiry_in_s", "100");
            OID4VCIssuerWellKnownProviderTest
                    .configureTestRealm(testClient, testRealm, clientAttributes, realmAttributes);
        }

    }

    public static class TestCredentialDefinitionInClientAttributes extends OID4VCTest {

        @Test
        public void testCredentialConfig() {
            OID4VCIssuerWellKnownProviderTest
                    .testCredentialConfig(suiteContext, testingClient);
        }

        @Override
        public void configureTestRealm(RealmRepresentation testRealm) {
            Map<String, String> clientAttributes = new HashMap<>(getTestCredentialDefinitionAttributes());
            Map<String, String> realmAttributes = new HashMap<>();
            OID4VCIssuerWellKnownProviderTest
                    .configureTestRealm(
                            getTestClient("did:web:test.org"),
                            testRealm,
                            clientAttributes,
                            realmAttributes
                    );
        }
    }

    public static class TestCredentialDefinitionInRealmAttributes extends OID4VCTest {

        @Test
        public void testCredentialConfig() {
            OID4VCIssuerWellKnownProviderTest
                    .testCredentialConfig(suiteContext, testingClient);
        }

        @Override
        public void configureTestRealm(RealmRepresentation testRealm) {
            Map<String, String> realmAttributes = new HashMap<>(getTestCredentialDefinitionAttributes());
            Map<String, String> clientAttributes = new HashMap<>();
            OID4VCIssuerWellKnownProviderTest.configureTestRealm(
                    getTestClient("did:web:test.org"),
                    testRealm,
                    clientAttributes,
                    realmAttributes
            );
        }
    }

    public static void configureTestRealm(
            ClientRepresentation testClient,
            RealmRepresentation testRealm,
            Map<String, String> clientAttributes,
            Map<String, String> realmAttributes
    ) {
        testClient.setAttributes(new HashMap<>(clientAttributes));
        testRealm.setAttributes(new HashMap<>(realmAttributes));
        extendConfigureTestRealm(testRealm, testClient);
    }

    public static void testCredentialConfig(SuiteContext suiteContext, KeycloakTestingClient testingClient) {
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
        String expectedCredentialsEndpoint = expectedIssuer + "/protocol/oid4vc/credential";
        final String expectedAuthorizationServer = expectedIssuer;
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
                    assertNotNull("The test-credential can optionally provide a claims claim.", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims());
                    assertNotNull("The test-credential claim firstName is present.", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims().get("firstName"));
                    assertFalse("The test-credential claim firstName is not mandatory.", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims().get("firstName").getMandatory());
                    assertEquals("The test-credential claim firstName shall be displayed as First Name", "First Name", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims().get("firstName").getDisplay().get(0).getName());
                    // moved sd-jwt specific config to org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCSdJwtIssuingEndpointTest.getConfig
                }));
    }

    public static void extendConfigureTestRealm(RealmRepresentation testRealm, ClientRepresentation clientRepresentation) {
        if (testRealm.getComponents() == null) {
            testRealm.setComponents(new MultivaluedHashMap<>());
        }

        testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(RSA_KEY));
        testRealm.getComponents().add("org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder", getCredentialBuilderProvider(Format.JWT_VC));


        if (testRealm.getClients() != null) {
            testRealm.getClients().add(clientRepresentation);
        } else {
            testRealm.setClients(new ArrayList<>(List.of(clientRepresentation)));
        }

        if (testRealm.getUsers() != null) {
            testRealm.getUsers().add(OID4VCTest.getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole"))));
        } else {
            testRealm.setUsers(new ArrayList<>(List.of(OID4VCTest.getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole"))))));
        }

        if (testRealm.getAttributes() != null) {
            testRealm.getAttributes().put("issuerDid", TEST_DID.toString());
        } else {
            testRealm.setAttributes(new HashMap<>(Map.of("issuerDid", TEST_DID.toString())));
        }
    }
}
