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

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.ClaimDisplay;
import org.keycloak.protocol.oid4vc.model.Claims;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequestEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.SIGNED_METADATA_JWT_TYPE;
import static org.keycloak.constants.Oid4VciConstants.BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE;
import static org.keycloak.jose.jwe.JWEConstants.A256GCM;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP_256;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.ATTR_ENCRYPTION_REQUIRED;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.ATTR_REQUEST_ZIP_ALGS;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.DEFLATE_COMPRESSION;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OID4VCIssuerWellKnownProviderTest extends OID4VCIssuerEndpointTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        Map<String, String> attributes = Optional.ofNullable(testRealm.getAttributes()).orElseGet(HashMap::new);
        attributes.put("credential_response_encryption.encryption_required", "true");
        attributes.put(ATTR_ENCRYPTION_REQUIRED, "true");
        attributes.put(BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE, "10");
        attributes.put(ATTR_REQUEST_ZIP_ALGS, DEFLATE_COMPRESSION);
        testRealm.setAttributes(attributes);

        if (testRealm.getComponents() == null) {
            testRealm.setComponents(new MultivaluedHashMap<>());
        }

        // Add encryption keys
        testRealm.getComponents().add("org.keycloak.keys.KeyProvider",
                getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100));
        testRealm.getComponents().add("org.keycloak.keys.KeyProvider",
                getRsaEncKeyProvider(RSA_OAEP, "enc-key-oaep", 101));

        super.configureTestRealm(testRealm);
    }

    @Test
    public void testUnsignedMetadata() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String wellKnownUri = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + TEST_REALM_NAME + "/.well-known/openid-credential-issuer";
            String expectedIssuer = getRealmPath(TEST_REALM_NAME);

            // Configure realm for unsigned metadata
            testingClient.server(TEST_REALM_NAME).run(session -> {
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ENABLED_ATTR, "false");
            });

            HttpGet getJsonMetadata = new HttpGet(wellKnownUri);
            getJsonMetadata.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            try (CloseableHttpResponse response = httpClient.execute(getJsonMetadata)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals("Content-Type should be application/json", MediaType.APPLICATION_JSON,
                        response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());
                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                CredentialIssuer issuer = JsonSerialization.readValue(json, CredentialIssuer.class);
                assertNotNull("Response should be a CredentialIssuer object", issuer);
                assertEquals("credential_issuer should be set", expectedIssuer, issuer.getCredentialIssuer());
                assertEquals("credential_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/credential",
                        issuer.getCredentialEndpoint());
                assertEquals("nonce_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/nonce",
                        issuer.getNonceEndpoint());
                assertEquals("deferred_credential_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/deferred_credential",
                        issuer.getDeferredCredentialEndpoint());
                assertNotNull("authorization_servers should be present", issuer.getAuthorizationServers());
                assertNotNull("credential_response_encryption should be present", issuer.getCredentialResponseEncryption());
                assertNotNull("batch_credential_issuance should be present", issuer.getBatchCredentialIssuance());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process JSON metadata response: " + e.getMessage(), e);
        }
    }

    @Test
    public void testSignedMetadata() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String wellKnownUri = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + TEST_REALM_NAME + "/.well-known/openid-credential-issuer";
            String expectedIssuer = getRealmPath(TEST_REALM_NAME);

            // Configure realm for signed metadata
            testingClient.server(TEST_REALM_NAME).run(session -> {
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ENABLED_ATTR, "true");
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ALG_ATTR, "RS256");
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_LIFESPAN_ATTR, "3600");
            });

            HttpGet getJwtMetadata = new HttpGet(wellKnownUri);
            getJwtMetadata.addHeader(HttpHeaders.ACCEPT, org.keycloak.utils.MediaType.APPLICATION_JWT);
            try (CloseableHttpResponse response = httpClient.execute(getJwtMetadata)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals("Content-Type should be application/jwt", org.keycloak.utils.MediaType.APPLICATION_JWT,
                        response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());
                String jws = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                assertNotNull("Response should be a JWT string", jws);
                JWSInput jwsInput = new JWSInput(jws);

                // Validate JOSE Header
                JWSHeader header = jwsInput.getHeader();
                assertEquals("Algorithm should be RS256", "RS256", header.getAlgorithm().name());
                assertEquals("Type should be openidvci-issuer-metadata+jwt",
                        SIGNED_METADATA_JWT_TYPE, header.getType());
                assertNotNull("Key ID should be present", header.getKeyId());
                assertNotNull("x5c header should be present if certificates are configured", header.getX5c());

                // Validate JWT claims
                Map<String, Object> claims = JsonSerialization.readValue(jwsInput.getContent(), Map.class);
                assertEquals("sub should match credential_issuer", expectedIssuer, claims.get("sub"));
                assertEquals("credential_issuer should be set", expectedIssuer, claims.get("credential_issuer"));
                assertEquals("iss should match credential_issuer", expectedIssuer, claims.get("iss"));
                assertNotNull("iat should be present", claims.get("iat"));
                assertTrue("iat should be a number", claims.get("iat") instanceof Number);
                assertTrue("iat should be recent", ((Number) claims.get("iat")).longValue() <= Time.currentTime());
                assertNotNull("exp should be present", claims.get("exp"));
                assertTrue("exp should be a number", claims.get("exp") instanceof Number);
                assertTrue("exp should be in the future",
                        ((Number) claims.get("exp")).longValue() > Time.currentTime());
                assertEquals("credential_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/credential",
                        claims.get("credential_endpoint"));
                assertEquals("nonce_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/nonce",
                        claims.get("nonce_endpoint"));
                assertEquals("deferred_credential_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/deferred_credential",
                        claims.get("deferred_credential_endpoint"));
                assertNotNull("authorization_servers should be present", claims.get("authorization_servers"));
                assertNotNull("credential_response_encryption should be present", claims.get("credential_response_encryption"));
                assertNotNull("batch_credential_issuance should be present", claims.get("batch_credential_issuance"));

                // Verify signature
                byte[] encodedSignatureInput = jwsInput.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
                byte[] signature = jwsInput.getSignature();
                testingClient.server(TEST_REALM_NAME).run(session -> {
                    RealmModel realm = session.getContext().getRealm();
                    KeyWrapper keyWrapper = session.keys().getActiveKey(realm, KeyUse.SIG, "RS256");
                    assertNotNull("Active signing key should exist", keyWrapper);
                    SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, "RS256");
                    assertNotNull("Signature provider should exist for RS256", signatureProvider);
                    SignatureVerifierContext verifier = signatureProvider.verifier(keyWrapper);
                    boolean isValid = verifier.verify(encodedSignatureInput, signature);
                    assertTrue("JWS signature should be valid", isValid);
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process JWT metadata response: " + e.getMessage(), e);
        }
    }

    @Test
    public void testUnsignedMetadataWhenSignedDisabled() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String wellKnownUri = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + TEST_REALM_NAME + "/.well-known/openid-credential-issuer";
            String expectedIssuer = getRealmPath(TEST_REALM_NAME);

            // Disable signed metadata
            testingClient.server(TEST_REALM_NAME).run(session -> {
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ENABLED_ATTR, "false");
                assertNotNull("Realm should have signed metadata disabled",
                        realm.getAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ENABLED_ATTR));
            });

            HttpGet getUnsignedMetadata = new HttpGet(wellKnownUri);
            getUnsignedMetadata.addHeader(HttpHeaders.ACCEPT, org.keycloak.utils.MediaType.APPLICATION_JWT);
            try (CloseableHttpResponse response = httpClient.execute(getUnsignedMetadata)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals("Content-Type should be application/json when signed metadata is disabled",
                        MediaType.APPLICATION_JSON, response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());
                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                CredentialIssuer issuer = JsonSerialization.readValue(json, CredentialIssuer.class);
                assertNotNull("Unsigned metadata should return CredentialIssuer", issuer);
                assertEquals("credential_issuer should be set", expectedIssuer, issuer.getCredentialIssuer());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process unsigned metadata response: " + e.getMessage(), e);
        }
    }

    @Test
    public void testSignedMetadataWithInvalidLifespan() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String wellKnownUri = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + TEST_REALM_NAME + "/.well-known/openid-credential-issuer";
            String expectedIssuer = getRealmPath(TEST_REALM_NAME);

            // Configure invalid lifespan
            testingClient.server(TEST_REALM_NAME).run(session -> {
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ENABLED_ATTR, "true");
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ALG_ATTR, "RS256");
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_LIFESPAN_ATTR, "invalid");
            });

            HttpGet getInvalidExpMetadata = new HttpGet(wellKnownUri);
            getInvalidExpMetadata.addHeader(HttpHeaders.ACCEPT, org.keycloak.utils.MediaType.APPLICATION_JWT);
            try (CloseableHttpResponse response = httpClient.execute(getInvalidExpMetadata)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals("Content-Type should be application/json due to invalid lifespan",
                        MediaType.APPLICATION_JSON, response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());
                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                CredentialIssuer issuer = JsonSerialization.readValue(json, CredentialIssuer.class);
                assertNotNull("Response should be a CredentialIssuer object", issuer);
                assertEquals("credential_issuer should be set", expectedIssuer, issuer.getCredentialIssuer());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process invalid lifespan metadata response: " + e.getMessage(), e);
        }
    }

    @Test
    public void testSignedMetadataWithInvalidAlgorithm() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String wellKnownUri = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + TEST_REALM_NAME + "/.well-known/openid-credential-issuer";
            String expectedIssuer = getRealmPath(TEST_REALM_NAME);

            // Configure invalid algorithm
            testingClient.server(TEST_REALM_NAME).run(session -> {
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ENABLED_ATTR, "true");
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ALG_ATTR, "INVALID_ALG");
                realm.setAttribute(OID4VCIssuerWellKnownProvider.SIGNED_METADATA_LIFESPAN_ATTR, "3600");
            });

            HttpGet getJwtMetadata = new HttpGet(wellKnownUri);
            getJwtMetadata.addHeader(HttpHeaders.ACCEPT, org.keycloak.utils.MediaType.APPLICATION_JWT);
            try (CloseableHttpResponse response = httpClient.execute(getJwtMetadata)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals("Content-Type should be application/json due to invalid algorithm",
                        MediaType.APPLICATION_JSON, response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());
                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                CredentialIssuer issuer = JsonSerialization.readValue(json, CredentialIssuer.class);
                assertNotNull("Response should be a CredentialIssuer object", issuer);
                assertEquals("credential_issuer should be set", expectedIssuer, issuer.getCredentialIssuer());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process invalid algorithm metadata response: " + e.getMessage(), e);
        }
    }

    /**
     * This test uses the configured scopes {@link #jwtTypeCredentialClientScope} and
     * {@link #sdJwtTypeCredentialClientScope} to verify that the metadata endpoint is presenting the expected data
     */
    @Test
    public void testMetaDataEndpointIsCorrectlySetup() {
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();

        assertEquals(getRealmPath(TEST_REALM_NAME), credentialIssuer.getCredentialIssuer());
        assertEquals(getBasePath(TEST_REALM_NAME) + OID4VCIssuerEndpoint.CREDENTIAL_PATH,
                credentialIssuer.getCredentialEndpoint());
        assertNull("Display was not configured", credentialIssuer.getDisplay());
        assertEquals("Authorization Server should have the realm-address.",
                1,
                credentialIssuer.getAuthorizationServers().size());
        assertEquals("Authorization Server should point to the realm-address.",
                getRealmPath(TEST_REALM_NAME),
                credentialIssuer.getAuthorizationServers().get(0));

        // Check credential_response_encryption
        CredentialResponseEncryptionMetadata encryption = credentialIssuer.getCredentialResponseEncryption();
        assertNotNull("credential_response_encryption should be present", encryption);
        assertEquals(List.of(RSA_OAEP, RSA_OAEP_256), encryption.getAlgValuesSupported());
        assertEquals(List.of(A256GCM), encryption.getEncValuesSupported());
        assertNotNull("zip_values_supported should be present", encryption.getZipValuesSupported());
        assertTrue("encryption_required should be true", encryption.getEncryptionRequired());

        // Check credential_request_encryption
        CredentialRequestEncryptionMetadata requestEncryption = credentialIssuer.getCredentialRequestEncryption();
        assertNotNull("credential_request_encryption should be present", requestEncryption);
        assertEquals(List.of(A256GCM), requestEncryption.getEncValuesSupported());
        assertNotNull("zip_values_supported should be present", requestEncryption.getZipValuesSupported());
        assertTrue("encryption_required should be true", requestEncryption.isEncryptionRequired());
        assertNotNull("JWKS should be present", requestEncryption.getJwks());

        CredentialIssuer.BatchCredentialIssuance batch = credentialIssuer.getBatchCredentialIssuance();
        assertNotNull("batch_credential_issuance should be present", batch);
        assertEquals(Integer.valueOf(10), batch.getBatchSize());

        for (ClientScopeRepresentation clientScope : List.of(jwtTypeCredentialClientScope,
                sdJwtTypeCredentialClientScope,
                minimalJwtTypeCredentialClientScope)) {
            compareMetadataToClientScope(credentialIssuer, clientScope);
        }
    }

    /**
     * This test will make sure that the default values are correctly added into the metadata endpoint
     */
    @Test
    public void testMinimalJwtCredentialHardcodedTest() {
        ClientScopeRepresentation clientScope = minimalJwtTypeCredentialClientScope;
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        SupportedCredentialConfiguration supportedConfig = credentialIssuer.getCredentialsSupported()
                .get(clientScope.getName());

        assertNotNull(supportedConfig);
        assertEquals(Format.SD_JWT_VC, supportedConfig.getFormat());
        assertEquals(clientScope.getName(), supportedConfig.getScope());
        assertEquals(1, supportedConfig.getCredentialDefinition().getType().size());
        assertEquals(clientScope.getName(), supportedConfig.getCredentialDefinition().getType().get(0));
        assertEquals(1, supportedConfig.getCredentialDefinition().getContext().size());
        assertEquals(clientScope.getName(), supportedConfig.getCredentialDefinition().getContext().get(0));
        assertNotNull(supportedConfig.getCredentialMetadata());
        assertEquals(clientScope.getName(), supportedConfig.getScope());

        compareClaims(supportedConfig.getFormat(), supportedConfig.getCredentialMetadata().getClaims(), clientScope.getProtocolMappers());
    }

    @Test
    public void testCredentialIssuerMetadataFields() {
        KeycloakTestingClient testingClient = this.testingClient;

        testingClient
                .server(TEST_REALM_NAME)
                .run(session -> {
                    CredentialIssuer issuer = getCredentialIssuer(session);

                    CredentialResponseEncryptionMetadata encryption = issuer.getCredentialResponseEncryption();
                    assertNotNull(encryption);

                    assertTrue(encryption.getAlgValuesSupported().contains(RSA_OAEP));
                    assertTrue("Supported encryption methods should include A256GCM", encryption.getEncValuesSupported().contains(A256GCM));
                    assertNotNull("zip_values_supported should be present", encryption.getZipValuesSupported());
                    assertTrue(encryption.getEncryptionRequired());

                    // Check credential_request_encryption
                    CredentialRequestEncryptionMetadata requestEncryption = issuer.getCredentialRequestEncryption();
                    assertNotNull("credential_request_encryption should be present", requestEncryption);
                    assertTrue("Supported encryption methods should include A256GCM", requestEncryption.getEncValuesSupported().contains(A256GCM));
                    assertNotNull("zip_values_supported should be present", requestEncryption.getZipValuesSupported());
                    assertTrue("encryption_required should be true", requestEncryption.isEncryptionRequired());
                    assertEquals(Integer.valueOf(10), issuer.getBatchCredentialIssuance().getBatchSize());

                    // Additional JWK checks from HEAD's testCredentialRequestEncryptionMetadataFields
                    assertNotNull(requestEncryption.getJwks());
                    JWK[] keys = requestEncryption.getJwks().getKeys();
                    assertEquals(4, keys.length); // Adjust based on actual key configuration
                    for (JWK jwk : keys) {
                        assertNotNull("JWK must have kid", jwk.getKeyId());
                        assertNotNull("JWK must have alg", jwk.getAlgorithm());
                        assertEquals("JWK must have use=enc", "enc", jwk.getPublicKeyUse());
                    }
                });
    }

    private static CredentialIssuer getCredentialIssuer(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();

        realm.setAttribute(ATTR_ENCRYPTION_REQUIRED, "true");
        realm.setAttribute(ATTR_REQUEST_ZIP_ALGS, DEFLATE_COMPRESSION);
        realm.setAttribute(BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE, "10");

        OID4VCIssuerWellKnownProvider provider = new OID4VCIssuerWellKnownProvider(session);
        return provider.getIssuerMetadata();
    }

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
                assertNotNull("zip_values_supported should be present",
                        oid4vciIssuerConfig.getCredentialResponseEncryption().getZipValuesSupported());
                assertTrue("Supported algorithms should include RSA-OAEP",
                        oid4vciIssuerConfig.getCredentialResponseEncryption().getAlgValuesSupported().contains("RSA-OAEP"));
                assertTrue("Supported encryption methods should include A256GCM",
                        oid4vciIssuerConfig.getCredentialResponseEncryption().getEncValuesSupported().contains("A256GCM"));
                assertNotNull("Credential request encryption should be advertised in metadata",
                        oid4vciIssuerConfig.getCredentialRequestEncryption());
                assertFalse("Supported encryption methods should not be empty",
                        oid4vciIssuerConfig.getCredentialRequestEncryption().getEncValuesSupported().isEmpty());
                assertNotNull("zip_values_supported should be present",
                        oid4vciIssuerConfig.getCredentialRequestEncryption().getZipValuesSupported());
                assertTrue("Supported encryption methods should include A256GCM",
                        oid4vciIssuerConfig.getCredentialRequestEncryption().getEncValuesSupported().contains("A256GCM"));
                assertNotNull("JWKS should be present in credential request encryption",
                        oid4vciIssuerConfig.getCredentialRequestEncryption().getJwks());
            }
        }
    }

    private void compareMetadataToClientScope(CredentialIssuer credentialIssuer, ClientScopeRepresentation clientScope) {
        String credentialConfigurationId = Optional.ofNullable(clientScope.getAttributes()
                        .get(CredentialScopeModel.CONFIGURATION_ID))
                .orElse(clientScope.getName());
        SupportedCredentialConfiguration supportedConfig = credentialIssuer.getCredentialsSupported()
                .get(credentialConfigurationId);
        assertNotNull("Configuration of type '" + credentialConfigurationId + "' must be present",
                supportedConfig);
        assertEquals(credentialConfigurationId, supportedConfig.getId());

        String expectedFormat = Optional.ofNullable(clientScope.getAttributes().get(CredentialScopeModel.FORMAT))
                .orElse(Format.SD_JWT_VC);
        assertEquals(expectedFormat, supportedConfig.getFormat());

        assertEquals(clientScope.getName(), supportedConfig.getScope());
        {
            // TODO this is still hardcoded
            assertEquals(1, supportedConfig.getCryptographicBindingMethodsSupported().size());
            assertEquals(CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT,
                    supportedConfig.getCryptographicBindingMethodsSupported().get(0));
        }

        compareDisplay(supportedConfig, clientScope);

        String expectedVct = Optional.ofNullable(clientScope.getAttributes().get(CredentialScopeModel.VCT))
                .orElse(clientScope.getName());
        assertEquals(expectedVct, supportedConfig.getVct());

        assertNotNull(supportedConfig.getCredentialDefinition());
        assertNotNull(supportedConfig.getCredentialDefinition().getType());
        List<String> credentialDefinitionTypes = Optional.ofNullable(clientScope.getAttributes()
                        .get(CredentialScopeModel.TYPES))
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElseGet(() -> List.of(clientScope.getName()));
        assertEquals(credentialDefinitionTypes.size(),
                supportedConfig.getCredentialDefinition().getType().size());

        MatcherAssert.assertThat(supportedConfig.getCredentialDefinition().getContext(),
                Matchers.containsInAnyOrder(credentialDefinitionTypes.toArray()));
        List<String> credentialDefinitionContexts = Optional.ofNullable(clientScope.getAttributes()
                        .get(CredentialScopeModel.CONTEXTS))
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElseGet(() -> List.of(clientScope.getName()));
        assertEquals(credentialDefinitionContexts.size(),
                supportedConfig.getCredentialDefinition().getContext().size());
        MatcherAssert.assertThat(supportedConfig.getCredentialDefinition().getContext(),
                Matchers.containsInAnyOrder(credentialDefinitionTypes.toArray()));

        List<String> signingAlgsSupported = new ArrayList<>(supportedConfig.getCredentialSigningAlgValuesSupported());
        String proofTypesSupportedString = supportedConfig.getProofTypesSupported().toJsonString();

        try {
            withCausePropagation(() -> testingClient.server(TEST_REALM_NAME).run((session -> {
                ProofTypesSupported expectedProofTypesSupported = ProofTypesSupported.parse(session,
                        List.of(Algorithm.RS256));
                assertEquals(expectedProofTypesSupported,
                        ProofTypesSupported.fromJsonString(proofTypesSupportedString));

                List<String> expectedSigningAlgs = OID4VCIssuerWellKnownProvider.getSupportedSignatureAlgorithms(session);
                MatcherAssert.assertThat(signingAlgsSupported,
                        Matchers.containsInAnyOrder(expectedSigningAlgs.toArray()));
            })));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        compareClaims(expectedFormat, supportedConfig.getCredentialMetadata().getClaims(), clientScope.getProtocolMappers());
    }

    private void compareDisplay(SupportedCredentialConfiguration supportedConfig, ClientScopeRepresentation clientScope) {
        String display = clientScope.getAttributes().get(CredentialScopeModel.VC_DISPLAY);
        if (StringUtil.isBlank(display)) {
            assertNull(supportedConfig.getCredentialMetadata() != null ? supportedConfig.getCredentialMetadata().getDisplay() : null);
            return;
        }
        List<DisplayObject> expectedDisplayObjectList;
        try {
            expectedDisplayObjectList = JsonSerialization.mapper.readValue(display, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        assertNotNull("Credential metadata should exist when display is configured", supportedConfig.getCredentialMetadata());
        assertEquals(expectedDisplayObjectList.size(), supportedConfig.getCredentialMetadata().getDisplay().size());
        MatcherAssert.assertThat("Must contain all expected display-objects",
                supportedConfig.getCredentialMetadata().getDisplay(),
                Matchers.containsInAnyOrder(expectedDisplayObjectList.toArray()));
    }

    /**
     * Each claim representation from the metadata is based on a protocol-mapper which we compare here
     */
    private void compareClaims(String credentialFormat,
                               Claims originalClaims,
                               List<ProtocolMapperRepresentation> originalProtocolMappers) {
        // the data must be serializable to transfer them to the server, so we convert the data to strings
        String claimsString = originalClaims.toJsonString();
        String protocolMappersString = toJsonString(originalProtocolMappers);

        try {
            withCausePropagation(() -> testingClient.server(TEST_REALM_NAME).run((session -> {
                Claims actualClaims = fromJsonString(claimsString, Claims.class);
                List<ProtocolMapperRepresentation> protocolMappers = fromJsonString(protocolMappersString,
                        new SerializableProtocolMapperReference());
                // check only protocol-mappers of type oid4vc
                protocolMappers = protocolMappers.stream().filter(protocolMapper -> {
                    return OID4VCLoginProtocolFactory.PROTOCOL_ID.equals(protocolMapper.getProtocol());
                }).toList();

                for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
                    OID4VCMapper mapper = (OID4VCMapper) session.getProvider(ProtocolMapper.class,
                            protocolMapper.getProtocolMapper());
                    ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
                    protocolMapperModel.setConfig(protocolMapper.getConfig());
                    mapper.setMapperModel(protocolMapperModel, credentialFormat);
                    Claim claim = actualClaims.stream()
                            .filter(c -> c.getPath().equals(mapper.getMetadataAttributePath()))
                            .findFirst().orElse(null);
                    if (mapper.includeInMetadata()) {
                        assertNotNull("There should be a claim matching the protocol-mappers config!", claim);
                    } else {
                        assertNull("This claim should not be included in the metadata-config!", claim);
                        // no other checks to do for this claim
                        continue;
                    }
                    assertEquals(claim.isMandatory(),
                            Optional.ofNullable(protocolMapper.getConfig()
                                            .get(Oid4vcProtocolMapperModel.MANDATORY))
                                    .map(Boolean::parseBoolean)
                                    .orElse(false));
                    String expectedDisplayString = protocolMapper.getConfig().get(Oid4vcProtocolMapperModel.DISPLAY);
                    List<ClaimDisplay> expectedDisplayList = fromJsonString(expectedDisplayString,
                            new SerializableClaimDisplayReference());
                    List<ClaimDisplay> actualDisplayList = claim.getDisplay();
                    if (expectedDisplayList == null) {
                        assertNull(actualDisplayList);
                    } else {
                        assertEquals(expectedDisplayList.size(), actualDisplayList.size());
                        MatcherAssert.assertThat(actualDisplayList,
                                Matchers.containsInAnyOrder(expectedDisplayList.toArray()));
                    }
                }
            })));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A jackson type-reference that can be used in the run-server-block
     */
    public static class SerializableProtocolMapperReference extends TypeReference<List<ProtocolMapperRepresentation>>
            implements Serializable {
    }

    /**
     * A jackson type-reference that can be used in the run-server-block
     */
    public static class SerializableClaimDisplayReference extends TypeReference<List<ClaimDisplay>>
            implements Serializable {
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
                    CredentialIssuer credentialIssuer = oid4VCIssuerWellKnownProvider.getIssuerMetadata();
                    assertEquals("The correct issuer should be included.", expectedIssuer, credentialIssuer.getCredentialIssuer());
                    assertEquals("The correct credentials endpoint should be included.", expectedCredentialsEndpoint, credentialIssuer.getCredentialEndpoint());
                    assertEquals("The correct deferred_credential_endpoint should be included.", expectedDeferredEndpoint, credentialIssuer.getDeferredCredentialEndpoint());
                    assertEquals("Since the authorization server is equal to the issuer, just 1 should be returned.", 1, credentialIssuer.getAuthorizationServers().size());
                    assertEquals("The expected server should have been returned.", expectedAuthorizationServer, credentialIssuer.getAuthorizationServers().get(0));
                    assertTrue("The test-credential should be supported.", credentialIssuer.getCredentialsSupported().containsKey("test-credential"));
                    assertEquals("The test-credential should offer type VerifiableCredential", "VerifiableCredential", credentialIssuer.getCredentialsSupported().get("test-credential").getScope());
                    assertEquals("The test-credential should be offered in the jwt-vc format.", Format.JWT_VC, credentialIssuer.getCredentialsSupported().get("test-credential").getFormat());
                    assertNotNull("The test-credential can optionally provide a claims claim.",
                            credentialIssuer.getCredentialsSupported().get("test-credential").getCredentialMetadata() != null ?
                                    credentialIssuer.getCredentialsSupported().get("test-credential").getCredentialMetadata().getClaims() : null);
                }));
    }

    @Test
    public void testBatchCredentialIssuanceValidation() {
        KeycloakTestingClient testingClient = this.testingClient;

        // Valid batch size (2 or greater) should be accepted
        testBatchSizeValidation(testingClient, "5", true, 5);

        // Invalid batch size (less than 2) should be rejected
        testBatchSizeValidation(testingClient, "1", false, null);

        // Edge case - batch size exactly 2 should be accepted
        testBatchSizeValidation(testingClient, "2", true, 2);

        // Zero batch size should be rejected
        testBatchSizeValidation(testingClient, "0", false, null);

        // Negative batch size should be rejected
        testBatchSizeValidation(testingClient, "-1", false, null);

        // Large valid batch size should be accepted
        testBatchSizeValidation(testingClient, "1000", true, 1000);

        // Non-numeric value should be rejected (parsing exception)
        testBatchSizeValidation(testingClient, "invalid", false, null);
    }

    @Test
    public void testOldOidcDiscoveryCompliantWellKnownUrlWithDeprecationHeaders() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            // Old OIDC Discovery compliant URL
            String oldWellKnownUri = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + TEST_REALM_NAME + "/.well-known/openid-credential-issuer";
            String expectedIssuer = getRealmPath(TEST_REALM_NAME);

            HttpGet getMetadata = new HttpGet(oldWellKnownUri);
            getMetadata.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = httpClient.execute(getMetadata)) {
                // Status & Content-Type
                assertEquals("Old well-known URL should return 200 OK",
                        HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

                String contentType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue();
                assertTrue("Content-Type should be application/json",
                        contentType.startsWith(MediaType.APPLICATION_JSON));

                // Headers
                Header warning = response.getFirstHeader("Warning");
                Header deprecation = response.getFirstHeader("Deprecation");
                Header link = response.getFirstHeader("Link");

                assertNotNull("Should have deprecation warning header", warning);
                assertTrue("Warning header should contain deprecation message",
                        warning.getValue().contains("Deprecated endpoint"));

                assertNotNull("Should have deprecation header", deprecation);
                assertEquals("Deprecation header should be 'true'", "true", deprecation.getValue());

                assertNotNull("Should have successor link header", link);
                assertTrue("Link header should contain successor-version",
                        link.getValue().contains("successor-version"));

                // Response body
                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                CredentialIssuer issuer = JsonSerialization.readValue(json, CredentialIssuer.class);

                assertNotNull("Response should be a CredentialIssuer object", issuer);

                assertEquals("credential_issuer should be set",
                        expectedIssuer, issuer.getCredentialIssuer());
                assertEquals("credential_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/credential", issuer.getCredentialEndpoint());
                assertEquals("nonce_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/nonce", issuer.getNonceEndpoint());
                assertEquals("deferred_credential_endpoint should be correct",
                        expectedIssuer + "/protocol/oid4vc/deferred_credential", issuer.getDeferredCredentialEndpoint());

                assertNotNull("authorization_servers should be present", issuer.getAuthorizationServers());
                assertNotNull("credential_response_encryption should be present", issuer.getCredentialResponseEncryption());
                assertNotNull("batch_credential_issuance should be present", issuer.getBatchCredentialIssuance());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process old well-known URL response: " + e.getMessage(), e);
        }
    }

    private void testBatchSizeValidation(KeycloakTestingClient testingClient, String batchSize, boolean shouldBePresent, Integer expectedValue) {
        testingClient
                .server(TEST_REALM_NAME)
                .run(session -> {
                    // Create a new isolated realm for testing
                    RealmModel testRealm = session.realms().createRealm("test-batch-validation-" + batchSize);

                    try {
                        testRealm.setAttribute(BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE, batchSize);

                        CredentialIssuer.BatchCredentialIssuance result = OID4VCIssuerWellKnownProvider.getBatchCredentialIssuance(testRealm);

                        if (shouldBePresent) {
                            assertNotNull("batch_credential_issuance should be present for batch size " + batchSize, result);
                            assertEquals("batch_credential_issuance should have correct batch size for " + batchSize,
                                    expectedValue, result.getBatchSize());
                        } else {
                            assertNull("batch_credential_issuance should be null for invalid batch size " + batchSize, result);
                        }
                    } finally {
                        session.realms().removeRealm(testRealm.getId());
                    }
                });
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
