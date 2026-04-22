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

package org.keycloak.tests.oid4vc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.VCFormat;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.ClaimDisplay;
import org.keycloak.protocol.oid4vc.model.Claims;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequestEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.KeyAttestationsRequired;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.SupportedProofTypeData;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testsuite.util.oauth.Endpoints;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.SIGNED_METADATA_JWT_TYPE;
import static org.keycloak.VCFormat.JWT_VC;
import static org.keycloak.VCFormat.SD_JWT_VC;
import static org.keycloak.common.crypto.CryptoConstants.A128KW;
import static org.keycloak.constants.OID4VCIConstants.BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE;
import static org.keycloak.jose.jwe.JWEConstants.A256GCM;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP_256;
import static org.keycloak.models.oid4vci.CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.ATTR_ENCRYPTION_REQUIRED;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.ATTR_REQUEST_ENCRYPTION_REQUIRED;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.ATTR_REQUEST_ZIP_ALGS;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.DEFLATE_COMPRESSION;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ALG_ATTR;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.SIGNED_METADATA_ENABLED_ATTR;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.SIGNED_METADATA_LIFESPAN_ATTR;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;


@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCIssuerWellKnownProviderTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();

        setRealmAttributes(Map.of(
                "credential_response_encryption.encryption_required", "true",
                ATTR_ENCRYPTION_REQUIRED, "true",
                ATTR_REQUEST_ENCRYPTION_REQUIRED, "true",
                BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE, "10",
                ATTR_REQUEST_ZIP_ALGS, DEFLATE_COMPRESSION
        ));

        ComponentsResource components = testRealm.admin().components();
        components.add(getRsaKeyProvider(getRsaKey_Default())).close();
        components.add(getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100)).close();
        components.add(getRsaEncKeyProvider(RSA_OAEP, "enc-key-oaep", 100)).close();
        components.add(getAesKeyProvider(A128KW, "aes-enc", "ENC", "aes-generated")).close();
        components.add(getAesKeyProvider(Algorithm.HS256, "aes-sig", "SIG", "hmac-generated")).close();
    }

    @Test
    public void testUnsignedMetadata() throws IOException {

        Endpoints endpoints = oauth.getEndpoints();
        String expectedIssuer = endpoints.getIssuer();

        Function<String, Boolean> run = uri -> {
            CredentialIssuerMetadataResponse response = oauth.oid4vc()
                    .issuerMetadataRequest()
                    .endpoint(uri)
                    .send();

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, response.getHeader(HttpHeaders.CONTENT_TYPE));

            CredentialIssuer issuer = response.getMetadata();
            assertNotNull(issuer, "Response should be a CredentialIssuer object");
            assertEquals(expectedIssuer, issuer.getCredentialIssuer());
            assertEquals(expectedIssuer + "/protocol/oid4vc/credential", issuer.getCredentialEndpoint());
            assertEquals(expectedIssuer + "/protocol/oid4vc/nonce", issuer.getNonceEndpoint());
            assertNull(issuer.getDeferredCredentialEndpoint(), "deferred_credential_endpoint should be omitted");
            assertNotNull(issuer.getAuthorizationServers(), "authorization_servers should be present");
            assertNotNull(issuer.getCredentialResponseEncryption(), "credential_response_encryption should be present");
            assertNotNull(issuer.getBatchCredentialIssuance(), "batch_credential_issuance should be present");
            return true;
        };

        assertTrue(run.apply(null), "IssuerMetadata on default endpoint URI");
        assertTrue(run.apply(endpoints.getOid4vcIssuerMetadata()), "IssuerMetadata on: " + endpoints.getOid4vcIssuerMetadata());
        assertTrue(run.apply(getSpecCompliantRealmMetadataPath()), "IssuerMetadata on: " + getSpecCompliantRealmMetadataPath());
    }

    @Test
    public void testSignedMetadata() {

        Endpoints endpoints = oauth.getEndpoints();
        String expectedIssuer = oauth.getEndpoints().getIssuer();

        Function<String, Boolean> run = uri -> {

            CredentialIssuerMetadataResponse response = oauth.oid4vc()
                    .issuerMetadataRequest()
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JWT)
                    .endpoint(uri)
                    .send();

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_JWT, response.getHeader(HttpHeaders.CONTENT_TYPE));

            JWSInput jwsInput = (JWSInput) response.getContent();
            assertNotNull(jwsInput, "Response should be JWSInput");

            Map<String, Object> claims;
            try {
                //noinspection unchecked
                claims = JsonSerialization.readValue(jwsInput.getContent(), Map.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Validate JOSE Header
            JWSHeader header = jwsInput.getHeader();
            assertEquals("RS256", header.getAlgorithm().name());
            assertEquals(SIGNED_METADATA_JWT_TYPE, header.getType());
            assertNotNull(header.getKeyId(), "Key ID should be present");
            assertNotNull(header.getX5c(), "x5c header should be present if certificates are configured");

            // Validate JWT claims
            assertEquals(expectedIssuer, claims.get("sub"), "sub should match credential_issuer");
            assertEquals(expectedIssuer, claims.get("iss"), "iss should match credential_issuer");
            assertNotNull(claims.get("iat"), "iat should be present");
            assertInstanceOf(Number.class, claims.get("iat"));
            assertTrue(((Number) claims.get("iat")).longValue() <= Time.currentTime(), "iat should be recent");
            assertNotNull(claims.get("exp"), "exp should be present");
            assertInstanceOf(Number.class, claims.get("exp"));
            assertTrue(((Number) claims.get("exp")).longValue() > Time.currentTime(), "exp should be in the future");
            assertEquals(expectedIssuer + "/protocol/oid4vc/credential", claims.get("credential_endpoint"));
            assertEquals(expectedIssuer + "/protocol/oid4vc/nonce", claims.get("nonce_endpoint"));
            assertFalse(claims.containsKey("deferred_credential_endpoint"), "deferred_credential_endpoint should be omitted");
            assertNotNull(claims.get("authorization_servers"), "authorization_servers should be present");
            assertNotNull(claims.get("credential_response_encryption"), "credential_response_encryption should be present");
            assertNotNull(claims.get("batch_credential_issuance"), "batch_credential_issuance should be present");

            // Verify signature
            byte[] encodedSignatureInput = jwsInput.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
            byte[] signature = jwsInput.getSignature();

            // [TODO] Verify metadata signature on client side
            runOnServer.run(session -> {
                RealmModel realm = session.getContext().getRealm();
                KeyWrapper keyWrapper = session.keys().getActiveKey(realm, KeyUse.SIG, "RS256");
                assertNotNull(keyWrapper, "Active signing key should exist");
                SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, "RS256");
                assertNotNull(signatureProvider, "Signature provider should exist for RS256");
                SignatureVerifierContext verifier = signatureProvider.verifier(keyWrapper);
                boolean isValid = verifier.verify(encodedSignatureInput, signature);
                assertTrue(isValid, "JWS signature should be valid");
            });
            return true;
        };

        assertTrue(run.apply(null), "IssuerMetadata on default endpoint URI");
        assertTrue(run.apply(endpoints.getOid4vcIssuerMetadata()), "IssuerMetadata on: " + endpoints.getOid4vcIssuerMetadata());
        assertTrue(run.apply(getSpecCompliantRealmMetadataPath()), "IssuerMetadata on: " + getSpecCompliantRealmMetadataPath());
    }

    @Test
    public void testSignedMetadataWithInvalidLifespan() throws IOException {

        Endpoints endpoints = oauth.getEndpoints();
        String expectedIssuer = endpoints.getIssuer();

        // Disable signed metadata
        setRealmAttributes(Map.of(
                SIGNED_METADATA_ALG_ATTR, "RS256",
                SIGNED_METADATA_LIFESPAN_ATTR, "invalid"
        ));

        CredentialIssuerMetadataResponse response = oauth.oid4vc()
                .issuerMetadataRequest()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JWT)
                .send();

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeader(HttpHeaders.CONTENT_TYPE));

        CredentialIssuer issuer = response.getMetadata();
        assertNotNull(issuer, "Response should be a CredentialIssuer object");
        assertEquals(expectedIssuer, issuer.getCredentialIssuer());

        // Reset signed metadata enabled
        setRealmAttributes(Map.of(SIGNED_METADATA_LIFESPAN_ATTR, "3600"));
    }

    @Test
    public void testSignedMetadataWithInvalidAlgorithm() throws IOException {

        Endpoints endpoints = oauth.getEndpoints();
        String expectedIssuer = endpoints.getIssuer();

        // Disable signed metadata
        setRealmAttributes(Map.of(
                SIGNED_METADATA_ALG_ATTR, "INVALID_ALG",
                SIGNED_METADATA_LIFESPAN_ATTR, "3600"
        ));

        CredentialIssuerMetadataResponse response = oauth.oid4vc()
                .issuerMetadataRequest()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JWT)
                .send();

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeader(HttpHeaders.CONTENT_TYPE));

        CredentialIssuer issuer = response.getMetadata();
        assertNotNull(issuer, "Response should be a CredentialIssuer object");
        assertEquals(expectedIssuer, issuer.getCredentialIssuer());

        // Reset signed metadata algorithm
        setRealmAttributes(Map.of(SIGNED_METADATA_ALG_ATTR, "RS256"));
    }

    @Test
    public void testSignedMetadataRequestedButDisabledReturnsJson() throws IOException {

        Endpoints endpoints = oauth.getEndpoints();
        String expectedIssuer = endpoints.getIssuer();

        // Explicitly disable signed metadata for this test case
        setRealmAttributes(Map.of(
                SIGNED_METADATA_ENABLED_ATTR, "false",
                SIGNED_METADATA_ALG_ATTR, "RS256",
                SIGNED_METADATA_LIFESPAN_ATTR, "3600"
        ));

        try {
            CredentialIssuerMetadataResponse response = oauth.oid4vc()
                    .issuerMetadataRequest()
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JWT)
                    .send();

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, response.getHeader(HttpHeaders.CONTENT_TYPE));

            CredentialIssuer issuer = response.getMetadata();
            assertNotNull(issuer, "Response should be unsigned CredentialIssuer JSON");
            assertEquals(expectedIssuer, issuer.getCredentialIssuer());
        } finally {
            // Restore signed metadata setting for other tests
            setRealmAttributes(Map.of(SIGNED_METADATA_ENABLED_ATTR, "true"));
        }
    }

    @Test
    public void testMetadataWithWildcardAcceptReturnsJson() throws IOException {
        String expectedIssuer = oauth.getEndpoints().getIssuer();

        CredentialIssuerMetadataResponse response = oauth.oid4vc()
                .issuerMetadataRequest()
                .header(HttpHeaders.ACCEPT, "*/*")
                .send();

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeader(HttpHeaders.CONTENT_TYPE));

        CredentialIssuer issuer = response.getMetadata();
        assertNotNull(issuer, "Response should be unsigned CredentialIssuer JSON");
        assertEquals(expectedIssuer, issuer.getCredentialIssuer());
    }

    @Test
    public void testMetadataWithCombinedAcceptPrefersJwtWhenEnabled() {
        String expectedIssuer = oauth.getEndpoints().getIssuer();

        // Ensure signed metadata is enabled for this negotiation test
        setRealmAttributes(Map.of(SIGNED_METADATA_ENABLED_ATTR, "true"));

        CredentialIssuerMetadataResponse response = oauth.oid4vc()
                .issuerMetadataRequest()
                .header(HttpHeaders.ACCEPT, "application/json, application/jwt")
                .send();

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JWT, response.getHeader(HttpHeaders.CONTENT_TYPE));

        JWSInput jwsInput = (JWSInput) response.getContent();
        assertNotNull(jwsInput, "Response should be signed metadata JWS");
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = JsonSerialization.readValue(jwsInput.getContent(), Map.class);
            assertEquals(expectedIssuer, claims.get("sub"), "sub should match credential_issuer");
            assertEquals(expectedIssuer, claims.get("iss"), "iss should match credential_issuer");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMetadataWithCaseInsensitiveJwtAcceptWhenDisabledReturnsJson() throws IOException {
        String expectedIssuer = oauth.getEndpoints().getIssuer();

        setRealmAttributes(Map.of(SIGNED_METADATA_ENABLED_ATTR, "false"));
        try {
            CredentialIssuerMetadataResponse response = oauth.oid4vc()
                    .issuerMetadataRequest()
                    .header(HttpHeaders.ACCEPT, "APPLICATION/JWT")
                    .send();

            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, response.getHeader(HttpHeaders.CONTENT_TYPE));

            CredentialIssuer issuer = response.getMetadata();
            assertNotNull(issuer, "Response should be unsigned CredentialIssuer JSON");
            assertEquals(expectedIssuer, issuer.getCredentialIssuer());
        } finally {
            setRealmAttributes(Map.of(SIGNED_METADATA_ENABLED_ATTR, "true"));
        }
    }

    /**
     * This test uses the configured scopes {@link #jwtTypeCredentialScope} and
     * {@link #sdJwtTypeCredentialScope} to verify that the metadata endpoint is presenting the expected data
     */
    @Test
    public void testMetaDataEndpointIsCorrectlySetup() throws Exception {

        Endpoints endpoints = oauth.getEndpoints();
        String expectedIssuer = endpoints.getIssuer();

        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .doIssuerMetadataRequest()
                .getMetadata();

        assertEquals(expectedIssuer, credentialIssuer.getCredentialIssuer());
        assertEquals(endpoints.getOid4vcCredential(), credentialIssuer.getCredentialEndpoint());
        assertNull(credentialIssuer.getDisplay(), "Display was not configured");
        assertEquals(1, credentialIssuer.getAuthorizationServers().size());
        assertEquals(expectedIssuer, credentialIssuer.getAuthorizationServers().get(0));

        // Check credential_response_encryption
        CredentialResponseEncryptionMetadata encryption = credentialIssuer.getCredentialResponseEncryption();
        assertNotNull(encryption, "credential_response_encryption should be present");
        List<String> algValuesSupported = encryption.getAlgValuesSupported();
        assertEquals(Set.of(RSA_OAEP, RSA_OAEP_256), new HashSet<>(algValuesSupported));
        assertEquals(List.of(A256GCM), encryption.getEncValuesSupported());
        assertNotNull(encryption.getZipValuesSupported(), "zip_values_supported should be present");
        assertTrue(encryption.getEncryptionRequired(), "encryption_required should be true");

        // Check credential_request_encryption
        CredentialRequestEncryptionMetadata requestEncryption = credentialIssuer.getCredentialRequestEncryption();
        assertNotNull(requestEncryption, "credential_request_encryption should be present");
        assertEquals(List.of(A256GCM), requestEncryption.getEncValuesSupported());
        assertNotNull(requestEncryption.getZipValuesSupported(), "zip_values_supported should be present");
        assertTrue(requestEncryption.isEncryptionRequired(), "encryption_required should be true");
        assertNotNull(requestEncryption.getJwks(), "JWKS should be present");

        CredentialIssuer.BatchCredentialIssuance batch = credentialIssuer.getBatchCredentialIssuance();
        assertNotNull(batch, "batch_credential_issuance should be present");
        assertEquals(Integer.valueOf(10), batch.getBatchSize());

        for (CredentialScopeRepresentation credScope : List.of(jwtTypeCredentialScope, sdJwtTypeCredentialScope, minimalJwtTypeCredentialScope)) {
            compareMetadataToClientScope(credentialIssuer, credScope);
        }
    }

    /**
     * This test will make sure that the default values are correctly added into the metadata endpoint
     */
    @Test
    public void testMinimalJwtCredentialHardcodedTest() {
        CredentialScopeRepresentation credScope = minimalJwtTypeCredentialScope;
        String credConfigurationId = credScope.getCredentialConfigurationId();

        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .doIssuerMetadataRequest()
                .getMetadata();

        SupportedCredentialConfiguration supportedConfig = credentialIssuer.getCredentialsSupported().get(credConfigurationId);

        assertNotNull(supportedConfig);
        assertEquals(SD_JWT_VC, supportedConfig.getFormat());
        assertEquals(credScope.getName(), supportedConfig.getScope());
        assertEquals(credScope.getName(), supportedConfig.getVct());
        assertNull(supportedConfig.getCredentialDefinition(), "SD-JWT credentials should not have credential_definition");
        assertNotNull(supportedConfig.getCredentialMetadata());

        compareClaims(supportedConfig.getFormat(), supportedConfig.getCredentialMetadata().getClaims(), credScope.getProtocolMappers());
    }

    @Test
    public void testCredentialIssuerMetadataFields() {

        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .doIssuerMetadataRequest()
                .getMetadata();

        CredentialResponseEncryptionMetadata encryption = credentialIssuer.getCredentialResponseEncryption();
        assertNotNull(encryption);

        assertTrue(encryption.getAlgValuesSupported().contains(RSA_OAEP));
        assertTrue(encryption.getEncValuesSupported().contains(A256GCM), "Supported encryption methods should include A256GCM");
        assertNotNull(encryption.getZipValuesSupported(), "zip_values_supported should be present");
        assertTrue(encryption.getEncryptionRequired());

        // Check credential_request_encryption
        CredentialRequestEncryptionMetadata requestEncryption = credentialIssuer.getCredentialRequestEncryption();
        assertNotNull(requestEncryption, "credential_request_encryption should be present");
        assertTrue(requestEncryption.getEncValuesSupported().contains(A256GCM), "Supported encryption methods should include A256GCM");
        assertNotNull(requestEncryption.getZipValuesSupported(), "zip_values_supported should be present");
        assertTrue(requestEncryption.isEncryptionRequired(), "encryption_required should be true");
        assertEquals(Integer.valueOf(10), credentialIssuer.getBatchCredentialIssuance().getBatchSize());

        // Additional JWK checks from HEAD's testCredentialRequestEncryptionMetadataFields
        assertNotNull(requestEncryption.getJwks());
        JWK[] keys = requestEncryption.getJwks().getKeys();
        assertTrue(keys.length >= 3, "At least three keys"); // Adjust based on actual key configuration
        for (JWK jwk : keys) {
            assertNotNull(jwk.getKeyId(), "JWK must have kid");
            assertNotNull(jwk.getAlgorithm(), "JWK must have alg");
            assertEquals("enc", jwk.getPublicKeyUse(), "JWK must have use=enc");
        }
    }

    @Test
    public void testIssuerMetadataIncludesEncryptionSupport() throws IOException {

        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .doIssuerMetadataRequest()
                .getMetadata();

        assertNotNull(credentialIssuer.getCredentialResponseEncryption(),
                "Encryption support should be advertised in metadata");
        assertFalse(credentialIssuer.getCredentialResponseEncryption().getAlgValuesSupported().isEmpty(),
                "Supported algorithms should not be empty");
        assertFalse(credentialIssuer.getCredentialResponseEncryption().getEncValuesSupported().isEmpty(),
                "Supported encryption methods should not be empty");
        assertNotNull(credentialIssuer.getCredentialResponseEncryption().getZipValuesSupported(),
                "zip_values_supported should be present");
        assertTrue(credentialIssuer.getCredentialResponseEncryption().getAlgValuesSupported().contains("RSA-OAEP"),
                "Supported algorithms should include RSA-OAEP");
        assertTrue(credentialIssuer.getCredentialResponseEncryption().getEncValuesSupported().contains("A256GCM"),
                "Supported encryption methods should include A256GCM");
        assertNotNull(credentialIssuer.getCredentialRequestEncryption(),
                "Credential request encryption should be advertised in metadata");
        assertFalse(credentialIssuer.getCredentialRequestEncryption().getEncValuesSupported().isEmpty(),
                "Supported encryption methods should not be empty");
        assertNotNull(credentialIssuer.getCredentialRequestEncryption().getZipValuesSupported(),
                "zip_values_supported should be present");
        assertTrue(credentialIssuer.getCredentialRequestEncryption().getEncValuesSupported().contains("A256GCM"),
                "Supported encryption methods should include A256GCM");
        assertNotNull(credentialIssuer.getCredentialRequestEncryption().getJwks(),
                "JWKS should be present in credential request encryption");
    }

    /**
     * When verifiable credentials are disabled for the realm, the OID4VCI well-known
     * endpoint must not be exposed.
     */
    @Test
    public void testWellKnownEndpointDisabledWhenVerifiableCredentialsOff() {

        setVerifiableCredentialsEnabled(false);
        try {
            CredentialIssuerMetadataResponse response = oauth.oid4vc()
                    .issuerMetadataRequest()
                    .send();

            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
            assertEquals("OID4VCI functionality is disabled for this realm", response.getError());

            IllegalStateException error = assertThrows(IllegalStateException.class, response::getMetadata);
            assertEquals("OID4VCI functionality is disabled for this realm", error.getMessage());

        } finally {
            setVerifiableCredentialsEnabled(true);
        }
    }

    @Test
    public void testBatchCredentialIssuanceValidation() {

        // Valid batch size (2 or greater) should be accepted
        testBatchSizeValidation("5", true, 5);

        // Invalid batch size (less than 2) should be rejected
        testBatchSizeValidation("1", false, null);

        // Edge case - batch size exactly 2 should be accepted
        testBatchSizeValidation("2", true, 2);

        // Zero batch size should be rejected
        testBatchSizeValidation("0", false, null);

        // Negative batch size should be rejected
        testBatchSizeValidation("-1", false, null);

        // Large valid batch size should be accepted
        testBatchSizeValidation("1000", true, 1000);

        // Non-numeric value should be rejected (parsing exception)
        testBatchSizeValidation("invalid", false, null);
    }

    @Test
    public void testOldOidcDiscoveryCompliantWellKnownUrlWithDeprecationHeaders() {

        // Old OIDC Discovery compliant URL
        String oldWellKnownUri = oauth.getBaseUrl() + "/realms/" + oauth.getRealm() + "/.well-known/" + OID4VCIssuerWellKnownProvider.PROVIDER_ID;
        String expectedIssuer = oauth.getEndpoints().getIssuer();

        CredentialIssuerMetadataResponse response = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(oldWellKnownUri)
                .send();

        // Status & Content-Type
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        String contentType = response.getHeader(HttpHeaders.CONTENT_TYPE);
        assertEquals(MediaType.APPLICATION_JSON, contentType);

        // Headers
        String warning = response.getHeader("Warning");
        assertNotNull(warning, "Should have deprecation warning header");
        assertTrue(warning.contains("Deprecated endpoint"), "Warning header should contain deprecation message");

        String deprecation = response.getHeader("Deprecation");
        assertNotNull(deprecation, "Should have deprecation header");
        assertEquals("true", deprecation, "Deprecation header should be 'true'");

        String link = response.getHeader("Link");
        assertNotNull(link, "Should have successor link header");
        assertTrue(link.contains("successor-version"), "Link header should contain successor-version");

        // Response body
        CredentialIssuer issuer = response.getMetadata();
        assertNotNull(issuer, "Response should be a CredentialIssuer object");

        assertEquals(expectedIssuer, issuer.getCredentialIssuer());
        assertEquals(expectedIssuer + "/protocol/oid4vc/credential", issuer.getCredentialEndpoint());
        assertEquals(expectedIssuer + "/protocol/oid4vc/nonce", issuer.getNonceEndpoint());
        assertNull(issuer.getDeferredCredentialEndpoint(), "deferred_credential_endpoint should be omitted");

        assertNotNull(issuer.getAuthorizationServers(), "authorization_servers should be present");
        assertNotNull(issuer.getCredentialResponseEncryption(), "credential_response_encryption should be present");
        assertNotNull(issuer.getBatchCredentialIssuance(), "batch_credential_issuance should be present");
    }

    @Test
    public void verifyDefaultCredentialConfigurations() throws IOException {

        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .doIssuerMetadataRequest()
                .getMetadata();

        Map<String, SupportedCredentialConfiguration> supported = credentialIssuer.getCredentialsSupported();
        String credType = "oid4vc_natural_person";
        for (String format : VCFormat.SUPPORTED_FORMATS) {
            String credConfigId = credType + VCFormat.getScopeSuffix(format);
            SupportedCredentialConfiguration credConfig = supported.get(credConfigId);
            assertNotNull(credConfig, "No " + credConfigId);
            assertEquals(credConfig.getId(), credConfig.getScope());
            assertEquals(format, credConfig.getFormat());
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void compareMetadataToClientScope(CredentialIssuer credentialIssuer, CredentialScopeRepresentation credScope) throws Exception {
        String credentialConfigurationId = credScope.getCredentialConfigurationId();
        SupportedCredentialConfiguration supportedConfig = credentialIssuer.getCredentialsSupported().get(credentialConfigurationId);
        assertNotNull(supportedConfig, "Configuration of type '" + credentialConfigurationId + "' must be present");
        assertEquals(credentialConfigurationId, supportedConfig.getId());

        String expectedFormat = credScope.getFormat();
        assertEquals(expectedFormat, supportedConfig.getFormat());

        assertEquals(credScope.getName(), supportedConfig.getScope());
        boolean bindingRequired = Boolean.parseBoolean(
                Optional.ofNullable(credScope.getAttributes())
                        .map(attrs -> attrs.get(CredentialScopeModel.VC_BINDING_REQUIRED))
                        .orElse("false")
        );
        if (bindingRequired) {
            assertNotNull(supportedConfig.getCryptographicBindingMethodsSupported(),
                    "Binding methods should be advertised when binding is required");
            assertEquals(1, supportedConfig.getCryptographicBindingMethodsSupported().size());
            assertEquals(CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT, supportedConfig.getCryptographicBindingMethodsSupported().get(0));
        } else {
            assertNull(supportedConfig.getCryptographicBindingMethodsSupported(),
                    "Binding methods should be omitted when binding is optional");
        }

        compareDisplay(supportedConfig, credScope);

        if (SD_JWT_VC.equals(expectedFormat)) {
            String expectedVct = Optional.ofNullable(credScope.getAttributes().get(CredentialScopeModel.VCT))
                    .orElse(credScope.getName());
            assertEquals(expectedVct, supportedConfig.getVct());
            assertNull(supportedConfig.getCredentialDefinition(), "SD-JWT credentials should not have credential_definition");
        } else if (JWT_VC.equals(expectedFormat)) {
            assertNull(supportedConfig.getVct(), "JWT_VC credentials should not have vct");
            assertNotNull(supportedConfig.getCredentialDefinition());
            assertNotNull(supportedConfig.getCredentialDefinition().getType());
            List<String> credentialDefinitionTypes = credScope.getSupportedCredentialTypes();
            if (!credentialDefinitionTypes.isEmpty()) {
                assertEquals(credentialDefinitionTypes.size(), supportedConfig.getCredentialDefinition().getType().size());
            }

            // @context must not be present for jwt_vc_json format per OID4VCI spec
            assertNull(supportedConfig.getCredentialDefinition().getContext(),
                    "jwt_vc_json credentials should not have @context in credential_definition");
        }

        List<String> signingAlgsSupported = supportedConfig.getCredentialSigningAlgValuesSupported();
        ProofTypesSupported proofTypesSupported = supportedConfig.getProofTypesSupported();
        if (!bindingRequired) {
            assertNull(proofTypesSupported, "proof_types_supported should be omitted when binding is optional");
            MatcherAssert.assertThat(signingAlgsSupported,
                    Matchers.containsInAnyOrder(getAllAsymmetricAlgorithms().toArray()));
            compareClaims(expectedFormat, supportedConfig.getCredentialMetadata().getClaims(), credScope.getProtocolMappers());
            return;
        }
        String proofTypesSupportedString = proofTypesSupported.toJsonString();

        MatcherAssert.assertThat(
                "JWT proof type must be supported",
                proofTypesSupported.getSupportedProofTypes().keySet(),
                Matchers.hasItem(ProofType.JWT)
        );
        MatcherAssert.assertThat(
                "Only configured proof types should be advertised",
                proofTypesSupported.getSupportedProofTypes().keySet(),
                Matchers.everyItem(Matchers.isOneOf(ProofType.JWT, ProofType.ATTESTATION))
        );

        List<String> expectedProofSigningAlgs = getAllAsymmetricAlgorithms();

        KeyAttestationsRequired expectedKeyAttestationsRequired;
        if (credScope.isKeyAttestationRequired()) {
            expectedKeyAttestationsRequired = new KeyAttestationsRequired();
            expectedKeyAttestationsRequired.setKeyStorage(credScope.getRequiredKeyAttestationKeyStorage());
            expectedKeyAttestationsRequired.setUserAuthentication(credScope.getRequiredKeyAttestationUserAuthentication());
        } else {
            expectedKeyAttestationsRequired = null;
        }
        String expectedKeyAttestationsRequiredString = JsonSerialization.valueAsString(expectedKeyAttestationsRequired);

        proofTypesSupported.getSupportedProofTypes().values()
                .forEach(proofTypeData -> {
                    assertEquals(expectedKeyAttestationsRequired, proofTypeData.getKeyAttestationsRequired());
                    MatcherAssert.assertThat(proofTypeData.getSigningAlgorithmsSupported(),
                            Matchers.containsInAnyOrder(expectedProofSigningAlgs.toArray()));
                });

        runOnServer.run(session -> {
            ProofTypesSupported actualProofTypesSupported = ProofTypesSupported.fromJsonString(proofTypesSupportedString);
            List<String> actualProofSigningAlgs = actualProofTypesSupported
                    .getSupportedProofTypes()
                    .get(ProofType.JWT)
                    .getSigningAlgorithmsSupported();

            KeyAttestationsRequired keyAttestationsRequired = //
                    Optional.ofNullable(expectedKeyAttestationsRequiredString)
                            .map(s -> JsonSerialization.valueFromString(s, KeyAttestationsRequired.class))
                            .orElse(null);

            // JWT proof support is mandatory for this flow. Attestation may or may not be advertised,
            // depending on credential configuration, so we validate JWT strictly and treat attestation as optional.
            SupportedProofTypeData jwtProofTypeData = actualProofTypesSupported
                    .getSupportedProofTypes()
                    .get(ProofType.JWT);
            assertNotNull(jwtProofTypeData, "JWT proof type must be present");
            assertEquals(keyAttestationsRequired, jwtProofTypeData.getKeyAttestationsRequired());
            MatcherAssert.assertThat(jwtProofTypeData.getSigningAlgorithmsSupported(),
                    Matchers.containsInAnyOrder(actualProofSigningAlgs.toArray()));

            SupportedProofTypeData attestationProofTypeData = actualProofTypesSupported
                    .getSupportedProofTypes()
                    .get(ProofType.ATTESTATION);
            if (attestationProofTypeData != null) {
                assertEquals(keyAttestationsRequired, attestationProofTypeData.getKeyAttestationsRequired());
                MatcherAssert.assertThat(attestationProofTypeData.getSigningAlgorithmsSupported(),
                        Matchers.containsInAnyOrder(actualProofSigningAlgs.toArray()));
            }

            MatcherAssert.assertThat(signingAlgsSupported,
                    Matchers.containsInAnyOrder(getAllAsymmetricAlgorithms().toArray()));
        });

        compareClaims(expectedFormat, supportedConfig.getCredentialMetadata().getClaims(), credScope.getProtocolMappers());
    }

    private static List<String> getAllAsymmetricAlgorithms() {
        return List.of(
                Algorithm.PS256, Algorithm.PS384, Algorithm.PS512,
                Algorithm.RS256, Algorithm.RS384, Algorithm.RS512,
                Algorithm.ES256, Algorithm.ES384, Algorithm.ES512,
                Algorithm.EdDSA);
    }

    private void compareDisplay(SupportedCredentialConfiguration supportedConfig, CredentialScopeRepresentation credScope) throws Exception {
        String display = credScope.getDisplay();
        if (StringUtil.isBlank(display)) {
            assertNull(supportedConfig.getCredentialMetadata() != null ? supportedConfig.getCredentialMetadata().getDisplay() : null);
            return;
        }
        List<DisplayObject> expectedDisplayObjectList = JsonSerialization.mapper.readValue(display, new TypeReference<>() {
        });

        assertNotNull(supportedConfig.getCredentialMetadata(), "Credential metadata should exist when display is configured");
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
        String protocolMappersString = JsonSerialization.valueAsString(originalProtocolMappers);

        runOnServer.run(session -> {
            Claims actualClaims = JsonSerialization.valueFromString(claimsString, Claims.class);
            ProtocolMapperRepresentation[] protocolMappersArr = Optional.ofNullable(protocolMappersString)
                    .map(v -> JsonSerialization.valueFromString(v, ProtocolMapperRepresentation[].class))
                    .orElse(new ProtocolMapperRepresentation[]{});
            // check only protocol-mappers of type oid4vc
            List<ProtocolMapperRepresentation> protocolMappers = Arrays.stream(protocolMappersArr)
                    .filter(protocolMapper -> OID4VCLoginProtocolFactory.PROTOCOL_ID.equals(protocolMapper.getProtocol()))
                    .toList();

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
                    assertNotNull(claim, "There should be a claim matching the protocol-mappers config!");
                } else {
                    assertNull(claim, "This claim should not be included in the metadata-config!");
                    // no other checks to do for this claim
                    continue;
                }
                assertEquals(claim.isMandatory(),
                        Optional.ofNullable(protocolMapper.getConfig()
                                        .get(Oid4vcProtocolMapperModel.MANDATORY))
                                .map(Boolean::parseBoolean)
                                .orElse(false));
                String expectedDisplayString = protocolMapper.getConfig().get(Oid4vcProtocolMapperModel.DISPLAY);
                ClaimDisplay[] expectedDisplayList = Optional.ofNullable(expectedDisplayString)
                        .map(v -> JsonSerialization.valueFromString(v, ClaimDisplay[].class))
                        .orElse(new ClaimDisplay[]{});
                List<ClaimDisplay> actualDisplayList = claim.getDisplay();
                if (expectedDisplayList.length == 0) {
                    assertNull(actualDisplayList);
                } else {
                    assertEquals(expectedDisplayList.length, actualDisplayList.size());
                    MatcherAssert.assertThat(actualDisplayList, Matchers.containsInAnyOrder(expectedDisplayList));
                }
            }
        });
    }

    private void testBatchSizeValidation(String batchSize, boolean shouldBePresent, Integer expectedValue) {
        runOnServer.run(session -> {
            // Create a new isolated realm for testing
            RealmModel testRealm = session.realms().createRealm("test-batch-validation-" + batchSize);

            try {
                testRealm.setAttribute(BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE, batchSize);

                CredentialIssuer.BatchCredentialIssuance result = OID4VCIssuerWellKnownProvider.getBatchCredentialIssuance(testRealm);

                if (shouldBePresent) {
                    assertNotNull(result, "batch_credential_issuance should be present for batch size " + batchSize);
                    assertEquals(expectedValue, result.getBatchSize(), "batch_credential_issuance should have correct batch size for " + batchSize);
                } else {
                    assertNull(result, "batch_credential_issuance should be null for invalid batch size " + batchSize);
                }
            } finally {
                session.realms().removeRealm(testRealm.getId());
            }
        });
    }

    private String getSpecCompliantRealmMetadataPath() {
        return oauth.getBaseUrl() + "/.well-known/" + OID4VCIssuerWellKnownProvider.PROVIDER_ID + "/realms/" + oauth.getRealm();
    }
}
