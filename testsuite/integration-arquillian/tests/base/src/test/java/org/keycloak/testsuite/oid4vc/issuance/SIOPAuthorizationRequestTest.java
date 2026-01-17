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
package org.keycloak.testsuite.oid4vc.issuance;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSAAlgorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.keys.KeyProvider;
import org.keycloak.keys.KeyProviderFactory;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequestBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.IDTokenRequest;
import org.keycloak.protocol.oid4vc.model.IDTokenRequestBuilder;
import org.keycloak.protocol.oid4vc.model.IDTokenResponse;
import org.keycloak.protocol.oid4vc.model.IDTokenResponseBuilder;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest;

import org.jboss.logging.Logger;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.keycloak.OID4VCConstants.USER_ATTRIBUTE_NAME_DID;
import static org.keycloak.util.DIDUtils.encodeDidKey;
import static org.keycloak.util.JsonSerialization.valueAsPrettyString;

import static org.junit.Assert.assertEquals;

/**
 * Tests the SIOPAuthenticator which implements the SIOP IDToken handshake.
 * <p/>
 * Details of the IDToken handshake between an OID4VCI Issuer and Holder are
 * described in the EBSI Conformance documentation.
 * <p/>
 * <a href="https://openid.net/specs/openid-connect-self-issued-v2-1_0.html">Self-Issued OpenID Provider v2</a>
 * <a href="https://hub.ebsi.eu/conformance/build-solutions/issue-to-holder-functional-flows#in-time-issuance">EBSI - Issue Verifiable Credentials</a>
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class SIOPAuthorizationRequestTest extends OID4VCIssuerEndpointTest {

    private static final Logger log = Logger.getLogger(SIOPAuthorizationRequestTest.class);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);

        // Add the Issuer's Key
        ComponentExportRepresentation issuerKeyProvider = new ComponentExportRepresentation();
        issuerKeyProvider.setName("ecdsa-issuer-key");
        issuerKeyProvider.setId("ecdsa-issuer-key-comp-id");
        issuerKeyProvider.setProviderId("ecdsa-generated");
        issuerKeyProvider.setConfig(new MultivaluedHashMap<>(Map.of(
                "algorithm", List.of("ES256"),
                "ecdsaEllipticCurveKey", List.of("P-256")))
        );
        testRealm.getComponents().add(KeyProvider.class.getName(), issuerKeyProvider);

        // Add the Holder's Key
        ComponentExportRepresentation userKeyProvider = new ComponentExportRepresentation();
        userKeyProvider.setName("ecdsa-holder-key");
        userKeyProvider.setId("ecdsa-holder-key-comp-id");
        userKeyProvider.setProviderId("ecdsa-generated");
        userKeyProvider.setConfig(new MultivaluedHashMap<>(Map.of(
                "algorithm", List.of("ES256"),
                "ecdsaEllipticCurveKey", List.of("P-256")))
        );
        testRealm.getComponents().add(KeyProvider.class.getName(), userKeyProvider);
    }

    @Test
    public void testDidKeyVerification() throws Exception {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session -> {
                    KeyPair keyPair = fixupHolderDidAttribute(session);

                    ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
                    String expDidKey = encodeDidKey(publicKey);

                    RealmModel realm = session.realms().getRealm(TEST_REALM_NAME);
                    UserModel alice = session.users().getUserByUsername(realm, "alice");
                    var wasDidKey = alice.getAttributes().get("did").get(0);
                    assertEquals(expDidKey, wasDidKey);
                });
    }

    @Test
    public void testIDTokenCreationAndVerification() throws Exception {
        String credConfigId = jwtTypeCredentialConfigurationIdName;
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session -> {
                    KeyPair keyPair = fixupHolderDidAttribute(session);

                    RealmModel realm = session.realms().getRealm(TEST_REALM_NAME);
                    OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    CredentialIssuer issuerMetadata = oid4VCIssuerWellKnownProvider.getIssuerMetadata();
                    OIDCWellKnownProvider oidcWellKnownProvider = new OIDCWellKnownProvider(session, Map.of(), true);
                    OIDCConfigurationRepresentation authMetadata = (OIDCConfigurationRepresentation) oidcWellKnownProvider.getConfig();

                    UserModel alice = session.users().getUserByUsername(realm, "alice");
                    var didKey = alice.getAttributes().get("did").get(0);

                    // Build an AuthorizationRequest as it would come in from an EBSI compliant Wallet
                    //
                    AuthorizationRequest authRequest = new AuthorizationRequestBuilder()
                            .withIssuer(issuerMetadata.getCredentialIssuer())
                            .withCredentialConfigurationIds(credConfigId)
                            .withRedirectUri("openid://redirect")
                            .withClientId(didKey)
                            .build();

                    String authRequestUrl = authRequest.toRequestUrl(authMetadata.getAuthorizationEndpoint());
                    log.infof("AuthorizationRequest: " + valueAsPrettyString(authRequest));
                    log.infof("AuthorizationRequestUrl: " + authRequestUrl);

                    // Issuer create an IDTokenRequest as a response to the Wallet's AuthorizationRequest
                    //
                    IDTokenRequest idTokenRequest = createIDTokenRequest(session, authRequest);
                    log.infof("IDTokenRequest: " + valueAsPrettyString(idTokenRequest));

                    // Wallet receives/verifies the IDTokenRequest
                    //
                    verifyIDTokenRequest(session, idTokenRequest);

                    // Wallet creates the IDTokenResponse
                    //
                    IDTokenResponse idTokenResponse = createIDTokenResponse(issuerMetadata, idTokenRequest, keyPair);
                    log.infof("IDTokenResponse: " + valueAsPrettyString(idTokenResponse));

                    // Issuer receives/verifies the IDTokenResponse
                    //
                    idTokenResponse.verify(session);
                    assertEquals(didKey, idTokenResponse.subject);
                });
    }

    private static KeyPair getHolderKeyPair(KeycloakSession session) {

        // Get KeyProvider component
        RealmModel realm = session.realms().getRealm(TEST_REALM_NAME);
        ComponentModel userKeyComp = realm.getComponentsStream(realm.getId(), KeyProvider.class.getName())
                .filter(cm -> cm.getName().equals("ecdsa-holder-key"))
                .findFirst().orElseThrow();

        // Load or generate the Key
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        KeyProviderFactory<?> providerFactory = (KeyProviderFactory<?>) sessionFactory.getProviderFactory(KeyProvider.class, userKeyComp.getProviderId());
        KeyProvider keyProvider = providerFactory.create(session, userKeyComp);
        KeyWrapper keyWrapper = keyProvider.getKeysStream().findFirst().orElseThrow();

        ECPublicKey publicKey = (ECPublicKey) keyWrapper.getPublicKey();
        ECPrivateKey privateKey = (ECPrivateKey) keyWrapper.getPrivateKey();
        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        
        return keyPair;
    }
    
    private static KeyPair fixupHolderDidAttribute(KeycloakSession session) {

        KeyPair keyPair = getHolderKeyPair(session);
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        String generatedDidKey = encodeDidKey(publicKey);

        RealmModel realm = session.realms().getRealm(TEST_REALM_NAME);
        UserModel alice = session.users().getUserByUsername(realm, "alice");
        String wasDidKey = alice.getAttributes().get(USER_ATTRIBUTE_NAME_DID).get(0);

        if (!wasDidKey.equals(generatedDidKey)) {
            log.infof("DID: %s", generatedDidKey);
            alice.setSingleAttribute(USER_ATTRIBUTE_NAME_DID, generatedDidKey);
        }
        return keyPair;
    }

    private static IDTokenRequest createIDTokenRequest(
            KeycloakSession session,
            AuthorizationRequest authRequest
    ) {

        Map<String, String> authRequestParams = new LinkedHashMap<>();
        authRequest.toRequestParameters().forEach((k, vals) -> authRequestParams.put(k, vals.get(0)));

        // Verify required query params
        for (String key : List.of("authorization_details", "client_id", "redirect_uri", "response_type")) {
            var val = Optional.ofNullable(authRequestParams.get(key)).orElse(null);
            if (StringUtil.isNullOrEmpty(val))
                throw new IllegalArgumentException("Required parameter: " + key);
        }

        OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
        CredentialIssuer issuerMetadata = oid4VCIssuerWellKnownProvider.getIssuerMetadata();
        OIDCWellKnownProvider oidcWellKnownProvider = new OIDCWellKnownProvider(session, Map.of(), true);
        OIDCConfigurationRepresentation authMetadata = (OIDCConfigurationRepresentation) oidcWellKnownProvider.getConfig();
        String redirectUri = authMetadata.getAuthorizationEndpoint() + "/direct_post";

        String issuer = authRequest.getAuthorizationDetails().stream()
                .flatMap(ad -> ad.getLocations().stream())
                .findFirst().orElse(null);

        if (!issuerMetadata.getCredentialIssuer().equals(issuer))
            throw new IllegalArgumentException("Unexpected issuer: " + issuer);

        String audience = authRequest.getClientId();
        if (!audience.startsWith("did:key:z"))
            throw new IllegalArgumentException("Unexpected audience: " + audience);

        RealmModel realm = session.realms().getRealm(TEST_REALM_NAME);
        IDTokenRequest idTokenRequest = new IDTokenRequestBuilder()
                .withClientId(issuer)
                .withRedirectUri(redirectUri)
                .withJwtIssuer(issuer)
                .withJwtAudience(audience)
                .buildAndSign(session, realm);
        return idTokenRequest;
    }

    private static void verifyIDTokenRequest(
            KeycloakSession session,
            IDTokenRequest idTokenRequest
    ) throws VerificationException {

        String encodedJwt = idTokenRequest.getRequest();
        RealmModel realm = session.realms().getRealm(TEST_REALM_NAME);

        try {
            JWSInput jws = new JWSInput(encodedJwt);
            String algo = jws.getHeader().getRawAlgorithm();

            // The Holder would normally access the Issuer's JWKS Endpoint Uri
            // using the kid from the Jwt header
            KeyManager keyManager = session.keys();
            KeyWrapper signingKey = keyManager.getActiveKey(realm, KeyUse.SIG, algo);
            PublicKey publicKey = (PublicKey) signingKey.getPublicKey();

            // Verify signature
            byte[] signedData = jws.getEncodedSignatureInput().getBytes(UTF_8);
            byte[] signature = jws.getSignature();

            switch (algo) {
                case Algorithm.ES256: {
                    Signature verifier = Signature.getInstance(JavaAlgorithm.ES256);
                    verifier.initVerify(publicKey);
                    verifier.update(signedData);

                    int expectedSize = ECDSAAlgorithm.getSignatureLength(algo);
                    byte[] der = ECDSAAlgorithm.concatenatedRSToASN1DER(signature, expectedSize);

                    boolean valid = verifier.verify(der);
                    if (!valid)
                        throw new VerificationException("Invalid ES256 signature");
                    break;
                }
                case Algorithm.RS256: {
                    Signature verifier = Signature.getInstance(JavaAlgorithm.RS256);
                    verifier.initVerify(publicKey);
                    verifier.update(signedData);
                    boolean valid = verifier.verify(signature);
                    if (!valid)
                        throw new VerificationException("Invalid RS256 signature");
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported algorithm");
            }
        } catch (JWSInputException | IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static IDTokenResponse createIDTokenResponse(
            CredentialIssuer issuerMetadata,
            IDTokenRequest idTokenRequest,
            KeyPair keyPair
    ) throws JWSInputException {
        String aud = issuerMetadata.getCredentialIssuer();
        String clientId = idTokenRequest.getClientId();

        ECPublicKey expPubKey = (ECPublicKey) keyPair.getPublic();
        String didKey = encodeDidKey(expPubKey);
        if (clientId.equals(didKey))
            throw new IllegalArgumentException("Unexpected IDToken client_id: " + clientId);

        IDTokenResponse idTokenResponse = new IDTokenResponseBuilder()
                .withJwtIssuer(didKey)
                .withJwtSubject(didKey)
                .withJwtAudience(aud)
                .buildAndSign(keyPair);

        return idTokenResponse;
    }
}
