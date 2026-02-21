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
package org.keycloak.tests.oid4vc;

import java.net.URI;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;
import static org.keycloak.common.util.KeyUtils.generateEcKeyPair;
import static org.keycloak.util.DIDUtils.decodeDidKey;
import static org.keycloak.util.DIDUtils.encodeDidKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the IDToken Authorization Handshake.
 * <p/>
 * Details of this Authorization flow are described in the EBSI Conformance documentation.
 * <p/>
 * <a href="https://openid.net/specs/openid-connect-self-issued-v2-1_0.html">Self-Issued OpenID Provider v2</a>
 * <a href="https://hub.ebsi.eu/conformance/build-solutions/issue-to-holder-functional-flows#in-time-issuance">EBSI - Issue Verifiable Credentials</a>
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
@KeycloakIntegrationTest(config = VCTestServerConfig.class)
public class IDTokenAuthorizationTest extends OID4VCIssuerTestBase {

    public static final String OPENID_REDIRECT_URI = "openid://redirect";

    OID4VCBasicWallet wallet;

    KeyPair subjectKeyPair;
    UserRepresentation holderRep;

    @BeforeEach
    void beforeEach() {
        wallet = new OID4VCBasicWallet(keycloak, oauth);

        // Generate the Holder's KeyPair
        subjectKeyPair = generateEcKeyPair(EC_KEY_SECP256R1);

        // Generate the Holder's DID
        ECPublicKey publicKey = (ECPublicKey) subjectKeyPair.getPublic();
        String appUserDid = encodeDidKey(publicKey);

        // Update the Holder's DID attribute
        holderRep = testRealm.admin().users().search("alice").get(0);
        holderRep.getAttributes().put(UserModel.DID, List.of(appUserDid));
        testRealm.admin().users().get(holderRep.getId()).update(holderRep);

        // Add redirect_uri=openid://redirect to client
        client.getRedirectUris().add(OPENID_REDIRECT_URI);
        testRealm.admin().clients().get(client.getId()).update(client);
    }

    @AfterEach
    void afterEach() {
        wallet.logout();
    }

    @Test
    public void testDidKeyVerification() throws Exception {
        holderRep = testRealm.admin().users().search("alice").get(0);
        Map<String, List<String>> holderAttributes = holderRep.getAttributes();
        var appUserDid = holderAttributes.get(UserModel.DID).get(0);
        ECPublicKey publicKey = decodeDidKey(appUserDid);
        assertEquals(subjectKeyPair.getPublic(), publicKey);
    }

    @Test
    public void testCredentialWithoutOffer() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);

        // Build an AuthorizationRequest with AuthorizationDetails
        //
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.credConfigId);
        authDetail.setLocations(List.of(issuerMetadata.getCredentialIssuer()));

        String userDid = holderRep.firstAttribute(UserModel.DID);

        // Build an AuthorizationRequest as it would come in from a compliant Wallet
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .client(userDid)
                .scope(ctx.credScopeName)
                .authorizationDetails(authDetail)
                .subjectKeyPair(subjectKeyPair)
                .issuerMetadata(issuerMetadata)
                .send();
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier,"No authorized credential identifier");

        // Build and send CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .send().getCredentialResponse();

        wallet.verifyCredentialsSignature(credResponse, Algorithm.PS384);
        Credential credential = credResponse.getCredentials().get(0);

        String encodedCredential = credential.getCredential().toString();
        JsonWebToken vcJwt = TokenVerifier.create(encodedCredential, JsonWebToken.class).getToken();
        assertEquals(userDid, vcJwt.getSubject());

        verifyCredentialResponse(ctx, ctx.holder, credResponse);
    }

    // Private ---------------------------------------------------------------------------------------------------------

    void verifyCredentialResponse(OID4VCTestContext ctx, String expUser, CredentialResponse credResponse) throws Exception {
        String scope = ctx.credentialScope.getName();
        String issuer = ctx.credentialScope.getIssuerDid();
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");

        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
        assertEquals(issuer, jsonWebToken.getIssuer());
        Object vc = jsonWebToken.getOtherClaims().get("vc");
        VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
        assertEquals(List.of(scope), credential.getType());
        assertEquals(URI.create(issuer), credential.getIssuer());
        assertEquals(expUser + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
    }
}
