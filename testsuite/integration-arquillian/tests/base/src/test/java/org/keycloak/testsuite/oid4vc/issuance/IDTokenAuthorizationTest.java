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
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequestBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialRequestBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.protocol.oid4vc.model.IDTokenRequest;
import org.keycloak.protocol.oid4vc.model.IDTokenResponse;
import org.keycloak.protocol.oid4vc.model.IDTokenResponseBuilder;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.PkceGenerator;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationRequestRequest;
import org.keycloak.testsuite.util.oauth.AuthorizationRequestResponse;

import org.junit.Before;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;
import static org.keycloak.OAuth2Constants.SCOPE_OPENID;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;
import static org.keycloak.common.util.KeyUtils.generateEcKeyPair;
import static org.keycloak.util.DIDUtils.decodeDidKey;
import static org.keycloak.util.DIDUtils.encodeDidKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    public static final String OPENID_REDIRECT_URI = "openid://redirect";

    class TestContext {
        String userDid;
        String username;
        KeyPair keyPair;
        CredentialIssuer issuerMetadata;
        SupportedCredentialConfiguration credentialConfig;
        OIDCConfigurationRepresentation authorizationMetadata;


        TestContext(String credConfigId, String username) {
            this.username = username;
            this.issuerMetadata = getCredentialIssuerMetadata();
            this.credentialConfig = issuerMetadata.getCredentialsSupported().get(credConfigId);
            this.authorizationMetadata = getAuthorizationMetadata(issuerMetadata.getAuthorizationServers().get(0));
        }
    }

    TestContext ctx;

    @Before
    public void setup() {
        super.setup();

        ctx = new TestContext(jwtTypeNaturalPersonScopeName, "alice");
        String credScope = ctx.credentialConfig.getScope();

        // Generate the Holder's KeyPair
        ctx.keyPair = generateEcKeyPair(EC_KEY_SECP256R1);

        // Generate the Holder's DID
        ECPublicKey publicKey = (ECPublicKey) ctx.keyPair.getPublic();
        ctx.userDid = encodeDidKey(publicKey);

        // Update the Holder's DID attribute
        List<UserRepresentation> userSearch = testRealm().users().search(ctx.username);
        UserRepresentation holderRepresentation = userSearch.get(0);
        holderRepresentation.getAttributes().put(UserModel.DID, List.of(ctx.userDid));
        testRealm().users().get(holderRepresentation.getId()).update(holderRepresentation);

        // Add redirect_uri=openid://redirect to client
        client.getRedirectUris().add(OPENID_REDIRECT_URI);
        testRealm().clients().get(client.getId()).update(client);

        List<String> clientScopes = client.getOptionalClientScopes();
        assertTrue("Optional scope '" + credScope + "' in: " + clientScopes, clientScopes.contains(credScope));
    }

    @Test
    public void testDidKeyVerification() throws Exception {
        UserRepresentation holderRepresentation = testRealm().users().search(ctx.username).get(0);
        Map<String, List<String>> holderAttributes = holderRepresentation.getAttributes();
        var wasDid = holderAttributes.get(UserModel.DID).get(0);
        assertEquals(ctx.userDid, wasDid);

        ECPublicKey wasPublicKey = decodeDidKey(wasDid);
        assertEquals(wasPublicKey, ctx.keyPair.getPublic());
    }

    @Test
    public void testCredentialWithoutOffer() throws Exception {

        String credConfigId = ctx.credentialConfig.getId();
        String credScope = ctx.credentialConfig.getScope();

        // Build an AuthorizationRequest with AuthorizationDetails
        //
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(ctx.issuerMetadata.getCredentialIssuer()));

        String userDid = findUser(ctx.username).firstAttribute(UserModel.DID);
        assertEquals(ctx.userDid, userDid);

        PkceGenerator pkce = PkceGenerator.s256();

        AuthorizationRequest authRequest = new AuthorizationRequestBuilder()
                .withClientId(userDid)
                // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
                .withScope(SCOPE_OPENID, credScope)
                .withAuthorizationDetail(authDetail)
                .withRedirectUri(OPENID_REDIRECT_URI)
                .withCodeChallenge(pkce)
                .build();

        AuthorizationRequestResponse authResponse = new AuthorizationRequestRequest(oauth, authRequest)
                .issuerMetadata(ctx.issuerMetadata)
                .subjectKeyPair(ctx.keyPair)
                .send();

        String authCode = authResponse.assertCode();
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .redirectUri(OPENID_REDIRECT_URI)
                .codeVerifier(pkce)
                .send();

        String accessToken = tokenResponse.getAccessToken();
        log.infof("AccessToken: %s", accessToken);

        CredentialRequest credRequest = new CredentialRequestBuilder()
                .withCredentialConfigurationId(credConfigId)
                .build();

        CredentialResponse credentialResponse = oauth.oid4vc().credentialRequest(credRequest)
                .bearerToken(accessToken)
                .send().getCredentialResponse();

        assertNotNull("No credentials", credentialResponse.getCredentials());
        assertFalse("Empty credentials array", credentialResponse.getCredentials().isEmpty());

        Credential credential = credentialResponse.getCredentials().get(0);
        assertNotNull("No credential", credential);

        verifyCredentialSignature(credential, Algorithm.PS384);
        JsonWebToken vcJwt = TokenVerifier.create(credential.getCredential().toString(), JsonWebToken.class).getToken();
        assertEquals(ctx.userDid, vcJwt.getSubject());
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private IDTokenResponse createIDTokenResponse(IDTokenRequest idTokenRequest) {
        String aud = ctx.issuerMetadata.getCredentialIssuer();
        String clientId = idTokenRequest.getClientId();

        ECPublicKey expPubKey = (ECPublicKey) ctx.keyPair.getPublic();
        String didKey = encodeDidKey(expPubKey);
        if (clientId.equals(didKey))
            throw new IllegalStateException("Unexpected IDToken client_id: " + clientId);

        IDTokenResponse idTokenResponse = new IDTokenResponseBuilder()
                .withJwtIssuer(didKey)
                .withJwtSubject(didKey)
                .withJwtAudience(aud)
                .sign(ctx.keyPair)
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
