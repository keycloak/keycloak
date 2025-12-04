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

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.common.util.DerUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequestBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialRequestBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.protocol.oid4vc.model.IDTokenRequest;
import org.keycloak.protocol.oid4vc.model.IDTokenRequestBuilder;
import org.keycloak.protocol.oid4vc.model.IDTokenResponse;
import org.keycloak.protocol.oid4vc.model.IDTokenResponseBuilder;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest;
import org.keycloak.testsuite.oid4vc.issuance.wallet.AuthorizationRequestGet;
import org.keycloak.testsuite.oid4vc.issuance.wallet.CredentialRequestPost;
import org.keycloak.testsuite.oid4vc.issuance.wallet.IDTokenResponsePost;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;
import static org.keycloak.OAuth2Constants.SCOPE_OPENID;
import static org.keycloak.OID4VCConstants.USER_ATTRIBUTE_NAME_DID;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;
import static org.keycloak.common.util.KeyUtils.generateEcKeyPair;
import static org.keycloak.util.DIDUtils.encodeDidKey;
import static org.keycloak.util.JsonSerialization.valueAsPrettyString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
public class IDTokenAuthorizationTest extends OID4VCIssuerEndpointTest {

    private static final Logger log = Logger.getLogger(IDTokenAuthorizationTest.class);

    public static final String OPENID_REDIRECT_URI = "openid://redirect";

    String credConfigId = jwtTypeNaturalPersonScopeName;

    TestContext ctx;

    static class TestContext {
        String appUsername;
        String appUserDid;
        String credConfigId;
        KeyPair appUserKeyPair;
        CredentialIssuer issuerMetadata;
        OIDCConfigurationRepresentation authorizationMetadata;
    }

    @Before
    public void setup() {
        super.setup();

        ctx = new TestContext();
        ctx.appUsername = "alice";
        ctx.credConfigId = jwtTypeNaturalPersonScopeName;
        ctx.issuerMetadata = getCredentialIssuerMetadata();
        ctx.authorizationMetadata = getAuthorizationMetadata(ctx.issuerMetadata.getAuthorizationServers().get(0));

        // Generate the Holder's KeyPair
        KeyPair keyPair = generateEcKeyPair(EC_KEY_SECP256R1);
        ctx.appUserKeyPair = keyPair;

        // Generate the Holder's DID
        ECPublicKey publicKey = (ECPublicKey) ctx.appUserKeyPair.getPublic();
        String appUserDid = encodeDidKey(publicKey);
        ctx.appUserDid = appUserDid;

        // Update the Holder's DID attribute
        UserRepresentation holderRepresentation = testRealm().users().search(ctx.appUsername).get(0);
        holderRepresentation.getAttributes().put(USER_ATTRIBUTE_NAME_DID, List.of(appUserDid));
        testRealm().users().get(holderRepresentation.getId()).update(holderRepresentation);

        // Add redirect_uri=openid://redirect to client
        client.getRedirectUris().add(OPENID_REDIRECT_URI);
        testRealm().clients().get(client.getId()).update(client);
    }

    @Test
    public void testDidKeyVerification() throws Exception {
        UserRepresentation holderRepresentation = testRealm().users().search(ctx.appUsername).get(0);
        Map<String, List<String>> holderAttributes = holderRepresentation.getAttributes();
        var wasAppUserDid = holderAttributes.get(USER_ATTRIBUTE_NAME_DID).get(0);
        assertEquals(ctx.appUserDid, wasAppUserDid);
    }

    @Test
    public void testCredentialWithoutOffer() throws Exception {

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.credConfigId);
        authDetail.setLocations(List.of(ctx.issuerMetadata.getCredentialIssuer()));

        UserRepresentation userRep = findUser(ctx.appUsername);
        String userDid = userRep.firstAttribute(USER_ATTRIBUTE_NAME_DID);

        // Build an AuthorizationRequest as it would come in from a compliant Wallet
        //
        AuthorizationRequest authRequest = new AuthorizationRequestBuilder()
                .withClientId(userDid)
                // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
                .withScope(SCOPE_OPENID, credConfigId)
                .withRedirectUri(OPENID_REDIRECT_URI)
                .withAuthorizationDetail(authDetail)
                .build();

        String authEndpoint = ctx.authorizationMetadata.getAuthorizationEndpoint();
        log.infof("AuthorizationRequest: %s", authRequest.toRequestUrl(authEndpoint));
        authRequest.toRequestParameters().forEach((k, v) -> log.infof("  %s=%s", k, v.get(0)));

        // Disable redirects for the HttpClients
        oauth.httpClient().set(HttpClients.custom().disableRedirectHandling().build());

        String authResponse = new AuthorizationRequestGet(oauth, authRequest).send();
        log.infof("AuthorizationResponse: %s", authResponse);
        new URIBuilder(authResponse).getQueryParams().forEach(p -> log.infof("  %s=%s", p.getName(), p.getValue()));

        // Wallet receives the IDTokenRequest
        //
        IDTokenRequest idTokenRequest = IDTokenRequestBuilder.fromUri(authResponse).build();

        // Wallet gets the Issuer's PublicKey to verify the IDTokenRequest signature
        //
        String kid = idTokenRequest.getJWSInput().getHeader().getKeyId();
        KeyMetadataRepresentation key = testRealm().keys().getKeyMetadata().getKeys().stream()
                .filter(kmd -> kmd.getKid().equals(kid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No key for: " + kid));
        PublicKey publicKey = DerUtils.decodePublicKey(key.getPublicKey(), key.getType());

        // Wallet verifies the IDTokenRequest
        //
        idTokenRequest.verify(publicKey);

        // Wallet creates/sends the IDTokenResponse
        //
        IDTokenResponse idTokenResponse = createIDTokenResponse(idTokenRequest);
        log.infof("IDTokenResponse: " + valueAsPrettyString(idTokenResponse));

        String codeResponse = new IDTokenResponsePost(oauth, idTokenRequest.getRedirectUri(), idTokenResponse).send();
        log.infof("CodeResponse: %s", codeResponse);
        List<NameValuePair> queryParams = new URIBuilder(codeResponse).getQueryParams();
        queryParams.forEach(p -> log.infof("  %s=%s", p.getName(), p.getValue()));

        // Extract the Authorization Code
        //
        String authCode = queryParams.stream()
                .filter(vp -> vp.getName().equals("code"))
                .map(NameValuePair::getValue)
                .findFirst().orElseThrow(() -> new IllegalStateException("No code"));

        // Redeem the Authorization Code for an AccessToken
        //
        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authCode)
                .redirectUri(authRequest.getRedirectUri())
                .send();
        String accessToken = accessTokenResponse.getAccessToken();
        log.infof("AccessToken: %s", accessToken);

        CredentialRequest credentialRequest = new CredentialRequestBuilder()
                .withCredentialConfigurationId(ctx.credConfigId)
                .build();

        String endpointUri = ctx.issuerMetadata.getCredentialEndpoint();
        CredentialResponse credentialResponse = new CredentialRequestPost(oauth, endpointUri, credentialRequest)
                .bearerToken(accessToken)
                .send();

        assertNotNull("No credentials", credentialResponse.getCredentials());
        assertFalse("Empty credentials array", credentialResponse.getCredentials().isEmpty());

        Credential credential = credentialResponse.getCredentials().get(0);
        assertNotNull("No credential", credential);

        verifyCredentialSignature(credential, Algorithm.PS384);
        JsonWebToken vcJwt = TokenVerifier.create(credential.getCredential().toString(), JsonWebToken.class).getToken();
        assertEquals(ctx.appUserDid, vcJwt.getSubject());
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private IDTokenResponse createIDTokenResponse(IDTokenRequest idTokenRequest) {
        String aud = ctx.issuerMetadata.getCredentialIssuer();
        String clientId = idTokenRequest.getClientId();

        KeyPair keyPair = ctx.appUserKeyPair;
        ECPublicKey expPubKey = (ECPublicKey) keyPair.getPublic();
        String didKey = encodeDidKey(expPubKey);
        if (clientId.equals(didKey))
            throw new IllegalStateException("Unexpected IDToken client_id: " + clientId);

        IDTokenResponse idTokenResponse = new IDTokenResponseBuilder()
                .withJwtIssuer(didKey)
                .withJwtSubject(didKey)
                .withJwtAudience(aud)
                .sign(keyPair)
                .build();

        return idTokenResponse;
    }

    private JWSInput verifyCredentialSignature(Credential credential, String signatureAlgorithm) throws Exception {
        String vcCredentialString = credential.getCredential().toString();
        JWSInput jwsInput = new JWSInput(vcCredentialString);
        JWSHeader header = jwsInput.getHeader();

        assertEquals(signatureAlgorithm, header.getRawAlgorithm());
        oauth.verifyToken(vcCredentialString, JsonWebToken.class);

        return jwsInput;
    }
}
