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
package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.TokenVerifier;
import org.keycloak.VCFormat;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.keybinding.CNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtCNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCGeneratedIdMapper;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.Claims;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Endpoint test with sd-jwt specific config.
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class OID4VCSdJwtIssuingEndpointTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testRequestTestCredential() {
        String scopeName = sdJwtTypeCredentialClientScope.getName();
        String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        testingClient
                .server(TEST_REALM_NAME)
                .run(session -> {
                    Proofs proof = new Proofs()
                            .setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));
                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);
                    testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                });
    }

    @Test
    public void testRequestTestCredentialWithKeybinding() {
        // Ensure SD-JWT signing uses a non-self-signed leaf cert for HAIP-6.1.1 compliance.
        testingClient.server(TEST_REALM_NAME).run(session ->
                ensureHaipCompliantCertificateChains(session));

        String cNonce = getCNonce();
        String scopeName = sdJwtTypeCredentialClientScope.getName();
        String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    Proofs proof = new Proofs()
                            .setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);

                    SdJwtVP sdJwtVP = testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                    assertNotNull(sdJwtVP.getCnfClaim(), "A cnf claim must be attached to the credential");
                }));
    }

    @Test
    public void testRequestTestCredentialWithInvalidKeybinding() throws Throwable {
        String cNonce = getCNonce();
        String scopeName = sdJwtTypeCredentialClientScope.getName();
        String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        try {
            withCausePropagation(() -> {
                testingClient.server(TEST_REALM_NAME).run((session -> {
                    Proofs proof = new Proofs()
                            .setJwt(List.of(generateInvalidJwtProof(getCredentialIssuer(session), cNonce)));

                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);

                    testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                }));
            });
            Assertions.fail("Should have thrown an exception");
        } catch (ErrorResponseException ex) {
            String message = ex.getErrorDescription();
            Assertions.assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
            Assertions.assertTrue(message.contains("Could not verify signature of provided proof"), "Unexpected: " + message);
        }
    }

    @Test
    public void testProofOfPossessionWithMissingAudience() throws Throwable {
        try {
            String scopeName = sdJwtTypeCredentialClientScope.getName();
            String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
            CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credConfigId);
            authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

            String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
            String token = tokenResponse.getAccessToken();
            List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
            String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

            final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                        final String nonceEndpoint = OID4VCIssuerWellKnownProvider.getNonceEndpoint(session.getContext());
                        // creates a cNonce with missing data
                        String cNonce = cNonceHandler.buildCNonce(null,
                                                                  Map.of(JwtCNonceHandler.SOURCE_ENDPOINT,
                                                                         nonceEndpoint));
                        Proofs proof = new Proofs().setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                        ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                                ClientScopeRepresentation.class);
                        testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                    })));
            Assertions.fail("Should have thrown an exception");
        } catch (ErrorResponseException ex) {
            String message = ex.getErrorDescription();
            Assertions.assertEquals(ErrorType.INVALID_NONCE.getValue(), ex.getError());
            Assertions.assertEquals("""
                    c_nonce: expected 'aud' to be equal to \
                    '[https://localhost:8543/auth/realms/test/protocol/oid4vc/credential]' but \
                    actual value was '[]'""", message);
        }
    }

    @Test
    public void testProofOfPossessionWithIllegalSourceEndpoint() throws Throwable {
        try {
            String scopeName = sdJwtTypeCredentialClientScope.getName();
            String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
            CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credConfigId);
            authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

            String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
            String token = tokenResponse.getAccessToken();
            List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
            String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

            final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                        final String credentialsEndpoint = //
                                OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
                        // creates a cNonce with missing data
                        String cNonce = cNonceHandler.buildCNonce(List.of(credentialsEndpoint), null);
                        Proofs proof = new Proofs().setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                        ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                                ClientScopeRepresentation.class);
                        testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                    })));
            Assertions.fail("Should have thrown an exception");
        } catch (ErrorResponseException ex) {
            String message = ex.getErrorDescription();
            Assertions.assertEquals(ErrorType.INVALID_NONCE.getValue(), ex.getError());
            Assertions.assertEquals("""
                    c_nonce: expected 'source_endpoint' to be equal to \
                    'https://localhost:8543/auth/realms/test/protocol/oid4vc/nonce' but \
                    actual value was 'null'""", message);
        }
    }

    @Test
    public void testProofOfPossessionWithExpiredState() throws Throwable {
        try {
            String scopeName = sdJwtTypeCredentialClientScope.getName();
            String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
            CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credConfigId);
            authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

            String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
            String token = tokenResponse.getAccessToken();
            List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
            String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

            final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                        final String credentialsEndpoint = //
                                OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
                        final String nonceEndpoint = OID4VCIssuerWellKnownProvider.getNonceEndpoint(session.getContext());
                        try {
                            // make the exp-value negative to set the exp-time in the past
                            session.getContext().getRealm().setAttribute(OID4VCIConstants.C_NONCE_LIFETIME_IN_SECONDS, -1);
                            String cNonce = cNonceHandler.buildCNonce(List.of(credentialsEndpoint),
                                                                      Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, nonceEndpoint));
                            Proofs proof = new Proofs().setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                            ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                                    ClientScopeRepresentation.class);
                            testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                        } finally {
                            // make sure other tests are not affected by the changed realm-attribute
                            session.getContext().getRealm().removeAttribute(OID4VCIConstants.C_NONCE_LIFETIME_IN_SECONDS);
                        }
                    })));
            Assertions.fail("Should have thrown an exception");
        } catch (ErrorResponseException ex) {
            String message = ex.getErrorDescription();
            Assertions.assertEquals(ErrorType.INVALID_NONCE.getValue(), ex.getError());
            Assertions.assertTrue(message.matches("c_nonce not valid: \\d+\\(exp\\) < \\d+\\(now\\)"),
                    String.format("Message '%s' should match regular expression", message));
        }
    }

    protected static String getCredentialIssuer(KeycloakSession session) {
        return OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
    }

    private static SdJwtVP testRequestTestCredential(KeycloakSession session, ClientScopeRepresentation clientScope,

                                                     String token, Proofs proof, String credentialIdentifier)
            throws VerificationException, IOException {

        // HAIP-6.1.1: ensure signing cert is not self-signed and trust anchor is not emitted in x5c.
        ensureHaipCompliantCertificateChains(session);

        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        authenticator.setTokenString(token);
        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

        CredentialRequest credentialRequest = new CredentialRequest()
                .setCredentialIdentifier(credentialIdentifier)
                .setProofs(proof);

        String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

        Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);
        assertEquals(HttpStatus.SC_OK,
                credentialResponse.getStatus(),
                "The credential request should be answered successfully.");
        assertNotNull(credentialResponse.getEntity(), "A credential should be responded.");
        CredentialResponse credentialResponseVO = JsonSerialization.mapper.convertValue(credentialResponse.getEntity(),
                CredentialResponse.class);
        new TestCredentialResponseHandler(sdJwtCredentialVct).handleCredentialResponse(credentialResponseVO,
                clientScope);

        // Get the credential from the credentials array
        return SdJwtVP.of(credentialResponseVO.getCredentials().get(0).getCredential().toString());
    }

    private static void ensureHaipCompliantCertificateChain(KeyWrapper keyWrapper) {
        if (keyWrapper == null) {
            throw new RuntimeException("Signing key is null");
        }
        if (keyWrapper.getPublicKey() == null || keyWrapper.getPrivateKey() == null) {
            throw new RuntimeException("Signing key missing key material for certificate generation. kid=" + keyWrapper.getKid());
        }

        List<X509Certificate> existingChain = keyWrapper.getCertificateChain();
        if (existingChain != null && !existingChain.isEmpty()) {
            X509Certificate signingCert = existingChain.get(0);
            if (signingCert != null
                    && !signingCert.getSubjectX500Principal().equals(signingCert.getIssuerX500Principal())) {
                return;
            }
        }

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair caKeyPair = kpg.generateKeyPair();
            X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "Test CA");

            KeyPair leafKeyPair = new KeyPair(
                    (PublicKey) keyWrapper.getPublicKey(),
                    (PrivateKey) keyWrapper.getPrivateKey()
            );
            X509Certificate leafCert = CertificateUtils.generateV3Certificate(
                    leafKeyPair,
                    caKeyPair.getPrivate(),
                    caCert,
                    "TestKey"
            );

            keyWrapper.setCertificateChain(List.of(leafCert, caCert));
            keyWrapper.setCertificate(leafCert);
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare HAIP-compliant certificate chain", e);
        }
    }

    private static void ensureHaipCompliantCertificateChains(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        KeyWrapper activeRs256 = session.keys().getActiveKey(realm, KeyUse.SIG, Algorithm.RS256);
        if (activeRs256 != null) {
            ensureHaipCompliantCertificateChain(activeRs256);
            return;
        }

        // Fallback for environments without an explicit active RS256 key.
        KeyWrapper fallback = session.keys()
                .getKeysStream(realm)
                .filter(k -> KeyUse.SIG.equals(k.getUse()))
                .filter(k -> Algorithm.RS256.equals(k.getAlgorithm()))
                .filter(k -> k.getPublicKey() != null && k.getPrivateKey() != null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No usable RS256 signing key configured in realm"));
        ensureHaipCompliantCertificateChain(fallback);
    }

    // Tests the complete flow from
    // 1. Retrieving the credential-offer-uri
    // 2. Using the uri to get the actual credential offer
    // 3. Get the issuer metadata
    // 4. Get the openid-configuration
    // 5. Get an access token for the pre-authorized code
    // 6. Get the credential
    @Test
    public void testCredentialIssuance() throws Exception {
        // Ensure SD-JWT signing uses a non-self-signed leaf cert for HAIP-6.1.1 compliance.
        testingClient.server(TEST_REALM_NAME).run(session ->
                ensureHaipCompliantCertificateChains(session));

        ClientScopeRepresentation clientScope = sdJwtTypeCredentialClientScope;
        String token = getBearerToken(oauth, client, clientScope.getName());

        // 1. Retrieving the credential-offer-uri
        final String credentialConfigurationId = clientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
        CredentialOfferURI credOfferUri = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .targetUser("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();

        assertNotNull(credOfferUri, "A valid offer uri should be returned");

        // 2. Using the uri to get the actual credential offer
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().doCredentialOfferRequest(credOfferUri);
        CredentialsOffer credentialsOffer = credentialOfferResponse.getCredentialsOffer();

        assertNotNull(credentialsOffer, "A valid offer should be returned");

        // 3. Get the issuer metadata
        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(credentialsOffer.getIssuerMetadataUrl())
                .send()
                .getMetadata();

        assertNotNull(credentialIssuer, "Issuer metadata should be returned");
        assertEquals(1, credentialIssuer.getAuthorizationServers().size(), "We only expect one authorization server.");

        // 4. Get the openid-configuration
        OIDCConfigurationRepresentation openidConfig = oauth
                .wellknownRequest()
                .url(credentialIssuer.getAuthorizationServers().get(0))
                .send()
                .getOidcConfiguration();

        assertNotNull(openidConfig.getTokenEndpoint(), "A token endpoint should be included.");
        assertTrue(openidConfig.getGrantTypesSupported().contains(PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE), "The pre-authorized code should be supported.");

        // 5. Get an access token for the pre-authorized code
        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(credentialsOffer.getPreAuthorizedCode())
                .endpoint(openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String theToken = accessTokenResponse.getAccessToken();
        assertNotNull(theToken, "Access token should be present");

        // Extract credential_identifier from authorization_details in token response
        List<OID4VCAuthorizationDetail> authDetailsResponse = accessTokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertFalse(authDetailsResponse.isEmpty(), "authorization_details should not be empty");
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "Credential identifier should be present");
        String cNonce = getCNonce();

        final String vct = clientScope.getAttributes().get(CredentialScopeModel.VCT);
        Proofs proof = new Proofs().setJwt(List.of(generateJwtProof(credentialIssuer.getCredentialIssuer(), cNonce)));

        // 6. Get the credential using credential_identifier (required when authorization_details are present)
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(proof)
                .bearerToken(theToken)
                .send();
        assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode(), "The credential request should be answered successfully.");
        assertNotNull(credentialResponse.getCredentialResponse(), "A credential should be responded.");

        new TestCredentialResponseHandler(vct).handleCredentialResponse(credentialResponse.getCredentialResponse(), clientScope);
    }

    /**
     * This is testing the configuration exposed by OID4VCIssuerWellKnownProvider based on the client and signing config setup here.
     */
    @Test
    public void testGetSdJwtConfigFromMetadata() {
        final String scopeName = sdJwtTypeCredentialClientScope.getName();
        final String credentialConfigurationId = sdJwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);
        final String verifiableCredentialType = sdJwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.VCT);
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
        String expectedCredentialsEndpoint = expectedIssuer + "/protocol/oid4vc/credential";
        String expectedNonceEndpoint = expectedIssuer + "/protocol/oid4vc/" + OID4VCIssuerEndpoint.NONCE_PATH;
        final String expectedAuthorizationServer = expectedIssuer;
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    CredentialIssuer credentialIssuer = oid4VCIssuerWellKnownProvider.getIssuerMetadata();
                    assertEquals(expectedIssuer, credentialIssuer.getCredentialIssuer(), "The correct issuer should be included.");
                    assertEquals(expectedCredentialsEndpoint, credentialIssuer.getCredentialEndpoint(), "The correct credentials endpoint should be included.");
                    assertEquals(expectedNonceEndpoint,
                            credentialIssuer.getNonceEndpoint(),
                            "The correct nonce endpoint should be included.");
                    assertEquals(1, credentialIssuer.getAuthorizationServers().size(), "Since the authorization server is equal to the issuer, just 1 should be returned.");
                    assertEquals(expectedAuthorizationServer, credentialIssuer.getAuthorizationServers().get(0), "The expected server should have been returned.");

                    assertTrue(credentialIssuer.getCredentialsSupported().containsKey(credentialConfigurationId),
                            "The sd-jwt-credential should be supported.");

                    SupportedCredentialConfiguration jwtVcConfig =
                            credentialIssuer.getCredentialsSupported().get(credentialConfigurationId);
                    assertEquals(scopeName,
                            jwtVcConfig.getScope(),
                            "The sd-jwt-credential should offer type test-credential");
                    assertEquals(VCFormat.SD_JWT_VC,
                            jwtVcConfig.getFormat(),
                            "The sd-jwt-credential should be offered in the jwt_vc format.");

                    assertNotNull(credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                          .getCredentialMetadata() != null ?
                                          credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                                  .getCredentialMetadata().getClaims() : null,
                                  "The sd-jwt-credential can optionally provide a claims claim.");

                    Claims jwtVcClaims = jwtVcConfig.getCredentialMetadata() != null ? jwtVcConfig.getCredentialMetadata().getClaims() : null;
                    assertNotNull(jwtVcClaims,
                            "The sd-jwt-credential can optionally provide a claims claim.");

                    assertEquals(7,  jwtVcClaims.size());
                    {
                        Claim claim = jwtVcClaims.get(0);
                        assertEquals(CLAIM_NAME_SUBJECT_ID, claim.getPath().get(0), "id claim is present");
                        assertFalse(claim.isMandatory(), "id claim not mandatory.");
                        assertNull(claim.getDisplay(), "id has no display value");
                    }
                    {
                        Claim claim = jwtVcClaims.get(1);
                        assertEquals("email", claim.getPath().get(0), "email claim is present");
                        assertFalse(claim.isMandatory(), "email claim not mandatory.");
                        assertNull(claim.getDisplay(), "email has no display value");
                    }
                    {
                        Claim claim = jwtVcClaims.get(2);
                        assertEquals("firstName", claim.getPath().get(0), "firstName claim is present");
                        assertFalse(claim.isMandatory(), "firstName claim not mandatory.");
                        assertNull(claim.getDisplay(), "firstName has no display value");
                    }
                    {
                        Claim claim = jwtVcClaims.get(3);
                        assertEquals("lastName", claim.getPath().get(0), "lastName claim is present");
                        assertFalse(claim.isMandatory(), "lastName claim not mandatory.");
                        assertNull(claim.getDisplay(), "lastName has no display value");
                    }
                    {
                        Claim claim = jwtVcClaims.get(4);
                        assertEquals(2,
                                claim.getPath().size(),
                                "address.street_address claim is nested.");
                        assertEquals("address",
                                claim.getPath().get(0),
                                "address.street_address claim has correct parent claim name.");
                        assertEquals("street_address",
                                claim.getPath().get(1),
                                "address.street_address claim has correct nested claim name.");
                        assertFalse(claim.isMandatory(),
                                "address.street_address claim is not mandatory.");
                        assertNull(claim.getDisplay(),
                                "address.street_address claim has no display value");
                    }
                    {
                        Claim claim = jwtVcClaims.get(5);
                        assertEquals(2,
                                claim.getPath().size(),
                                "address.locality claim is nested.");
                        assertEquals("address",
                                claim.getPath().get(0),
                                "address.locality claim has correct parent claim name.");
                        assertEquals("locality",
                                claim.getPath().get(1),
                                "address.locality claim has correct nested claim name.");
                        assertFalse(claim.isMandatory(),
                                "address.locality claim is not mandatory.");
                        assertNull(claim.getDisplay(),
                                "address.locality claim has no display value");
                    }
                    {
                        Claim claim = jwtVcClaims.get(6);
                        assertEquals("scope-name", claim.getPath().get(0), "scope-name claim is present");
                        assertFalse(claim.isMandatory(), "scope-name claim not mandatory.");
                        assertNull(claim.getDisplay(), "scope-name has no display value");
                    }

                    assertEquals(verifiableCredentialType,
                            credentialIssuer.getCredentialsSupported().get(credentialConfigurationId).getVct(),
                            "The sd-jwt-credential should offer vct");

                    assertNotNull(credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                    .getCryptographicBindingMethodsSupported(),
                            "Cryptographic binding methods should be advertised for SD-JWT test credential");
                    assertTrue(credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                    .getCredentialSigningAlgValuesSupported().contains("ES256"),
                            "The sd-jwt-credential should contain a credential signing algorithm named ES256");

                    SupportedCredentialConfiguration sdJwtConfig =
                            credentialIssuer.getCredentialsSupported().get(credentialConfigurationId);
                    assertNotNull(sdJwtConfig.getProofTypesSupported(),
                            "Proof types should be advertised when binding is required");
                    assertNotNull(sdJwtConfig.getProofTypesSupported()
                                    .getSupportedProofTypes()
                                    .get("jwt"),
                            "JWT proof type should be present for SD-JWT test credential");
                    assertTrue(sdJwtConfig.getProofTypesSupported()
                                    .getSupportedProofTypes()
                                    .get("jwt")
                                    .getSigningAlgorithmsSupported()
                                    .contains("ES256"),
                            "The sd-jwt-credential should support a proof of type jwt with signing algorithm ES256");
                    assertEquals(credentialConfigurationId,
                                 credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                         .getCredentialMetadata() != null &&
                                         credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                                 .getCredentialMetadata().getDisplay() != null ?
                                         credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                                 .getCredentialMetadata().getDisplay().get(0).getName() : null,
                                 "The sd-jwt-credential should display as Test Credential");
                }));
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session,
                                                                AppAuthManager.BearerTokenAuthenticator authenticator) {
        JwtCredentialBuilder testJwtCredentialBuilder = new JwtCredentialBuilder(new StaticTimeProvider(5), session);
        SdJwtCredentialBuilder testSdJwtCredentialBuilder = new SdJwtCredentialBuilder();

        return new OID4VCIssuerEndpoint(
                session,
                Map.of(
                        testSdJwtCredentialBuilder.getSupportedFormat(), testSdJwtCredentialBuilder,
                        testJwtCredentialBuilder.getSupportedFormat(), testJwtCredentialBuilder
                ),
                authenticator,
                TIME_PROVIDER,
                30);
    }

    private static final String JTI_KEY = "jti";

    public static ProtocolMapperRepresentation getJtiGeneratedIdMapper() {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("generated-id-mapper");
        protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        protocolMapperRepresentation.setId(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocolMapper("oid4vc-generated-id-mapper");
        protocolMapperRepresentation.setConfig(Map.of(
                OID4VCGeneratedIdMapper.CLAIM_NAME, JTI_KEY
        ));
        return protocolMapperRepresentation;
    }

    public static ClientScopeModel createCredentialScope(KeycloakSession session) {
        RealmModel realmModel = session.getContext().getRealm();
        ClientScopeModel credentialScope = session.clientScopes()
                .addClientScope(realmModel, jwtTypeCredentialScopeName);
        credentialScope.setAttribute(CredentialScopeModel.VC_IDENTIFIER,
                jwtTypeCredentialScopeName);
        credentialScope.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        return credentialScope;
    }

    @Override
    protected ComponentExportRepresentation getKeyProvider() {
        return getEcKeyProvider();
    }

    static class TestCredentialResponseHandler extends CredentialResponseHandler {
        final String vct;

        TestCredentialResponseHandler(String vct) {
            this.vct = vct;
        }

        @Override
        protected void handleCredentialResponse(CredentialResponse credentialResponse, ClientScopeRepresentation clientScope) throws VerificationException {
            // SDJWT have a special format.
            SdJwtVP sdJwtVP = SdJwtVP.of(credentialResponse.getCredentials().get(0).getCredential().toString());
            JsonWebToken jsonWebToken = TokenVerifier.create(sdJwtVP.getIssuerSignedJWT().getJws(), JsonWebToken.class).getToken();

            assertNotNull(jsonWebToken, "A valid credential string should have been responded");
            assertNotNull(jsonWebToken.getId(), "The credentials should include the id claim");
            assertNotNull(jsonWebToken.getOtherClaims().get("vct"), "The credentials should be included at the vct-claim.");
            assertEquals(vct, jsonWebToken.getOtherClaims().get("vct").toString(), "The credentials should be included at the vct-claim.");

            Map<String, JsonNode> disclosureMap = sdJwtVP.getDisclosures().values().stream()
                    .map(disclosure -> {
                        try {
                            JsonNode jsonNode = JsonSerialization.mapper.readTree(Base64Url.decode(disclosure));
                            return Map.entry(jsonNode.get(1).asText(), jsonNode); // Create a Map.Entry
                        } catch (IOException e) {
                            throw new RuntimeException(e); // Re-throw as unchecked exception
                        }
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            assertFalse(disclosureMap.containsKey("given_name"), "Only mappers supported for the requested type should have been evaluated.");
            assertTrue(disclosureMap.containsKey("firstName"), "The credentials should include the firstName claim.");
            assertEquals("John", disclosureMap.get("firstName").get(2).asText(), "firstName claim incorrectly mapped.");
            assertTrue(disclosureMap.containsKey("lastName"), "The credentials should include the lastName claim.");
            assertEquals("Doe", disclosureMap.get("lastName").get(2).asText(), "lastName claim incorrectly mapped.");

            assertThat("address is parent claim for nested claims", disclosureMap.get("address").get(2), instanceOf(ObjectNode.class));
            ObjectNode nestedAddressClaim = (ObjectNode) disclosureMap.get("address").get(2);
            assertEquals(2, nestedAddressClaim.size(), "address contains two nested claims");
            assertEquals("221B Baker Street", nestedAddressClaim.get("street_address").asText(), "street_address mapped correctly");
            assertEquals("London", nestedAddressClaim.get("locality").asText(), "locality mapped correctly");

            assertTrue(disclosureMap.containsKey("scope-name"),
                    "The credentials should include the scope-name claim.");
            assertEquals(clientScope.getName(),
                    disclosureMap.get("scope-name").get(2).textValue(),
                    "The credentials should include the scope-name claims correct value.");
            assertTrue(disclosureMap.containsKey("email"), "The credentials should include the email claim.");
            assertEquals("john@email.cz", disclosureMap.get("email").get(2).asText(), "email claim incorrectly mapped.");

            assertNotNull(jsonWebToken.getIat(), "Test credential shall include an iat claim.");
            assertNotNull(jsonWebToken.getNbf(), "Test credential shall include an nbf claim.");

        }
    }
}
