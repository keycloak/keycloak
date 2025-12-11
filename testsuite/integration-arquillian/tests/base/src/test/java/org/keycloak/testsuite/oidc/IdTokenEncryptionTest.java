/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.oidc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.AesCbcHmacShaContentEncryptionProvider;
import org.keycloak.crypto.AesGcmContentEncryptionProvider;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.RsaCekManagementProvider;
import org.keycloak.jose.JOSEHeader;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IdTokenEncryptionTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true).setServiceAccountsEnabled(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.client("test-app", "password");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA1_5EncA128CBC_HS256() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-256", adminClient, testContext);
        testIdTokenSignatureAndEncryption(Algorithm.ES256, JWEConstants.RSA1_5, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA1_5EncA192CBC_HS384() {
        testIdTokenSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA1_5, JWEConstants.A192CBC_HS384);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA1_5EncA256CBC_HS512() {
        testIdTokenSignatureAndEncryption(Algorithm.PS384, JWEConstants.RSA1_5, JWEConstants.A256CBC_HS512);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA1_5EncA128GCM() {
        testIdTokenSignatureAndEncryption(Algorithm.RS384, JWEConstants.RSA1_5, JWEConstants.A128GCM);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA1_5EncA192GCM() {
        testIdTokenSignatureAndEncryption(Algorithm.RS512, JWEConstants.RSA1_5, JWEConstants.A192GCM);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA1_5EncA256GCM() {
        testIdTokenSignatureAndEncryption(Algorithm.RS256, JWEConstants.RSA1_5, JWEConstants.A256GCM);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncA128CBC_HS256() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-521", adminClient, testContext);
        testIdTokenSignatureAndEncryption(Algorithm.ES512, JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncA192CBC_HS384() {
        testIdTokenSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA_OAEP, JWEConstants.A192CBC_HS384);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncA256CBC_HS512() {
        testIdTokenSignatureAndEncryption(Algorithm.PS512, JWEConstants.RSA_OAEP, JWEConstants.A256CBC_HS512);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEP256EncA128CBC_HS256() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-521", adminClient, testContext);
        testIdTokenSignatureAndEncryption(Algorithm.ES512, JWEConstants.RSA_OAEP_256, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEP256EncA192CBC_HS384() {
        testIdTokenSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA_OAEP_256, JWEConstants.A192CBC_HS384);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEP256EncA256CBC_HS512() {
        testIdTokenSignatureAndEncryption(Algorithm.PS512, JWEConstants.RSA_OAEP_256, JWEConstants.A256CBC_HS512);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncA128GCM() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-256", adminClient, testContext);
        testIdTokenSignatureAndEncryption(Algorithm.ES256, JWEConstants.RSA_OAEP, JWEConstants.A128GCM);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncA192GCM() {
        testIdTokenSignatureAndEncryption(Algorithm.PS384, JWEConstants.RSA_OAEP, JWEConstants.A192GCM);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncA256GCM() {
        testIdTokenSignatureAndEncryption(Algorithm.PS512, JWEConstants.RSA_OAEP, JWEConstants.A256GCM);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncDefault() {
        testIdTokenSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA_OAEP, null);
    }

    private void testIdTokenSignatureAndEncryption(String sigAlgorithm, String algAlgorithm, String encAlgorithm) {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            // generate and register encryption key onto client
            TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
            oidcClientEndpointsResource.generateKeys(algAlgorithm);

            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            // set id token signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenSignedResponseAlg(sigAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseAlg(algAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseEnc(encAlgorithm);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);

            // get id token
            AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
            String code = response.getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            // parse JWE and JOSE Header
            String jweStr = tokenResponse.getIdToken();
            String[] parts = jweStr.split("\\.");
            Assert.assertEquals(parts.length, 5);

            // get decryption key
            // not publickey , use privateKey
            Map<String, String> keyPair = oidcClientEndpointsResource.getKeysAsPem();
            PrivateKey decryptionKEK = PemUtils.decodePrivateKey(keyPair.get("privateKey"));

            // a nested JWT (signed and encrypted JWT) needs to set "JWT" to its JOSE Header's "cty" field
            JWEHeader jweHeader = (JWEHeader) getHeader(parts[0]);
            Assert.assertEquals("JWT", jweHeader.getContentType());

            // verify and decrypt JWE
            if (encAlgorithm == null) encAlgorithm = JWEConstants.A128CBC_HS256;
            JWEAlgorithmProvider algorithmProvider = getJweAlgorithmProvider(algAlgorithm);
            JWEEncryptionProvider encryptionProvider = getJweEncryptionProvider(encAlgorithm);
            byte[] decodedString = TokenUtil.jweKeyEncryptionVerifyAndDecode(decryptionKEK, jweStr, algorithmProvider, encryptionProvider);
            String idTokenString = new String(decodedString, StandardCharsets.UTF_8);

            // verify JWS
            IDToken idToken = oauth.verifyIDToken(idTokenString);
            Assert.assertEquals("test-user@localhost", idToken.getPreferredUsername());
            Assert.assertEquals("test-app", idToken.getIssuedFor());
        } catch (JWEException e) {
            Assert.fail();
        } finally {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            // revert id token signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenSignedResponseAlg(Algorithm.RS256);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseAlg(null);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseEnc(null);
            // revert jwks_url settings
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(false);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(null);
            clientResource.update(clientRep);
        }
    }

    private JWEAlgorithmProvider getJweAlgorithmProvider(String algAlgorithm) {
        return new RsaCekManagementProvider(null, algAlgorithm).jweAlgorithmProvider();
    }
    private JWEEncryptionProvider getJweEncryptionProvider(String encAlgorithm) {
        JWEEncryptionProvider jweEncryptionProvider = null;
        switch(encAlgorithm) {
            case JWEConstants.A128GCM:
            case JWEConstants.A192GCM:
            case JWEConstants.A256GCM:
                jweEncryptionProvider = new AesGcmContentEncryptionProvider(null, encAlgorithm).jweEncryptionProvider();
                break;
            case JWEConstants.A128CBC_HS256:
            case JWEConstants.A192CBC_HS384:
            case JWEConstants.A256CBC_HS512:
                jweEncryptionProvider = new AesCbcHmacShaContentEncryptionProvider(null, encAlgorithm).jweEncryptionProvider();
                break;
        }
        return jweEncryptionProvider;
    }

    private JOSEHeader getHeader(String base64Header) {
        try {
            byte[] decodedHeader = Base64Url.decode(base64Header);
            return JsonSerialization.readValue(decodedHeader, JWEHeader.class);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testIdTokenEncryptionWithoutEncryptionKEK() throws Exception{
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            // generate and register signing/verifying key onto client, not encryption key
            TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
            oidcClientEndpointsResource.generateKeys(Algorithm.RS256);

            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            // set id token signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenSignedResponseAlg(Algorithm.RS256);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseAlg(JWEConstants.RSA1_5);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseEnc(JWEConstants.A128CBC_HS256);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);

            // get id token but failed
            AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
            AccessTokenResponse atr = oauth.doAccessTokenRequest(response.getCode());
            Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, atr.getError());
            Assert.assertEquals("can not get encryption KEK", atr.getErrorDescription());

            // get id token but failed with client_credentials grant type
            oauth.scope("openid");
            AccessTokenResponse responseClientCredentials = oauth.client(clientRep.getClientId(), clientRep.getSecret()).doClientCredentialsGrantAccessTokenRequest();
            Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, responseClientCredentials.getError());
            Assert.assertEquals("can not get encryption KEK", responseClientCredentials.getErrorDescription());
        } finally {
            // Revert
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenSignedResponseAlg(Algorithm.RS256);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseAlg(null);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseEnc(null);
            // Revert jwks_url settings
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(false);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(null);
            clientResource.update(clientRep);
        }
    }

}
