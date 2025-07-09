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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProviderFactory;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryption;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
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

        @Test
        public void testCredentialIssuerMetadataFields() {
            String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
            KeycloakTestingClient testingClient = this.testingClient;

            testingClient
                    .server(TEST_REALM_NAME)
                    .run(session -> {
                        // Setup test realm attributes
                        RealmModel realm = session.getContext().getRealm();
                        realm.setAttribute("oid4vci.encryption.algs", "RSA-OAEP");
                        realm.setAttribute("oid4vci.encryption.encs", "A256GCM");
                        realm.setAttribute("oid4vci.encryption.required", "true");
                        realm.setAttribute("batch_credential_issuance.batch_size", "10");
                        realm.setAttribute("signed_metadata", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJmb28iOiJiYXIifQ.XYZ123abc");

                        OID4VCIssuerWellKnownProvider provider = new OID4VCIssuerWellKnownProvider(session);
                        Object config = provider.getConfig();
                        assertTrue("Should return CredentialIssuer", config instanceof CredentialIssuer);
                        CredentialIssuer issuer = (CredentialIssuer) config;

                        // Check basic endpoints
                        assertEquals(expectedIssuer, issuer.getCredentialIssuer());
                        assertNotNull(issuer.getCredentialEndpoint());
                        assertNotNull(issuer.getNonceEndpoint());
                        assertNotNull(issuer.getDeferredCredentialEndpoint());
                        assertEquals(List.of(expectedIssuer), issuer.getAuthorizationServers());

                        // Check credential_response_encryption
                        CredentialResponseEncryptionMetadata encryption = issuer.getCredentialResponseEncryption();
                        assertNotNull("credential_response_encryption should be present", encryption);
                        assertEquals(List.of("RSA-OAEP"), encryption.getAlgValuesSupported());
                        assertEquals(List.of("A256GCM"), encryption.getEncValuesSupported());
                        assertTrue("encryption_required should be true", encryption.getEncryptionRequired());

                        // Check batch_credential_issuance
                        CredentialIssuer.BatchCredentialIssuance batch = issuer.getBatchCredentialIssuance();
                        assertNotNull("batch_credential_issuance should be present", batch);
                        assertEquals(Integer.valueOf(10), batch.getBatchSize());

                        // Check signed_metadata
                        assertEquals(
                                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJmb28iOiJiYXIifQ.XYZ123abc",
                                issuer.getSignedMetadata()
                        );

                        // Check credentials_supported is not empty
                        assertNotNull(issuer.getCredentialsSupported());
                        assertFalse(issuer.getCredentialsSupported().isEmpty());
                    });
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
        realmAttributes.put("credential_response_encryption.alg_values_supported", "[\"RSA-OAEP\"]");
        realmAttributes.put("credential_response_encryption.enc_values_supported", "[\"A256GCM\"]");
        realmAttributes.put("credential_response_encryption.encryption_required", "true");
        realmAttributes.put("batch_credential_issuance.batch_size", "10");
        realmAttributes.put("signed_metadata", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJmb28iOiJiYXIifQ.XYZ123abc"); // example JWT
        testClient.setAttributes(new HashMap<>(clientAttributes));
        testRealm.setAttributes(new HashMap<>(realmAttributes));
        extendConfigureTestRealm(testRealm, testClient);
    }

    public static class TestEncryptionMetadata extends OID4VCTest {

        @Test
        public void testIssuerMetadataIncludesEncryptionSupport() throws IOException {
            try (Client client = AdminClientUtil.createResteasyClient()) {
                UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
                URI oid4vciDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder)
                        .build(TEST_REALM_NAME, OID4VCIssuerWellKnownProviderFactory.PROVIDER_ID);
                WebTarget oid4vciDiscoveryTarget = client.target(oid4vciDiscoveryUri);

                try (Response discoveryResponse = oid4vciDiscoveryTarget.request().get()) {
                    CredentialIssuer oid4vciIssuerConfig = JsonSerialization.readValue(
                            discoveryResponse.readEntity(String.class), CredentialIssuer.class);

                    assertNotNull("Encryption support should be advertised in metadata",
                            oid4vciIssuerConfig.getCredentialResponseEncryption());
                    assertFalse("Supported algorithms should not be empty",
                            oid4vciIssuerConfig.getCredentialResponseEncryption().getAlgValuesSupported().isEmpty());
                    assertFalse("Supported encryption methods should not be empty",
                            oid4vciIssuerConfig.getCredentialResponseEncryption().getEncValuesSupported().isEmpty());
                }
            }
        }

        @Override
        public void configureTestRealm(RealmRepresentation testRealm) {
            // Configure realm with encryption support if needed
            Map<String, String> realmAttributes = new HashMap<>();
            realmAttributes.put("oid4vci.encryption.algs", "RSA-OAEP,RSA-OAEP-256");
            realmAttributes.put("oid4vci.encryption.encs", "A256GCM,A128CBC-HS256");
            testRealm.setAttributes(realmAttributes);

            extendConfigureTestRealm(testRealm, getTestClient("did:web:test.org"));
        }
    }

    public static void testCredentialConfig(SuiteContext suiteContext, KeycloakTestingClient testingClient) {
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
        String expectedCredentialsEndpoint = expectedIssuer + "/protocol/oid4vc/credential";
        String expectedDeferredEndpoint = expectedIssuer + "/protocol/oid4vc/deferred_credential";
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
                    assertEquals("The correct deferred_credential_endpoint should be included.", expectedDeferredEndpoint, credentialIssuer.getDeferredCredentialEndpoint());
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
