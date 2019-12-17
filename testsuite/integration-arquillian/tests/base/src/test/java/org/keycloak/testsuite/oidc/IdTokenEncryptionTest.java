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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.AesCbcHmacShaContentEncryptionProvider;
import org.keycloak.crypto.AesGcmContentEncryptionProvider;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.RsaCekManagementProvider;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.util.TokenUtil;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

public class IdTokenEncryptionTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
        oauth.maxAge(null);
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
    public void testIdTokenEncryptionAlgRSA1_5EncA128GCM() {
        testIdTokenSignatureAndEncryption(Algorithm.RS384, JWEConstants.RSA1_5, JWEConstants.A128GCM);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncA128CBC_HS256() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-521", adminClient, testContext);
        testIdTokenSignatureAndEncryption(Algorithm.ES512, JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testIdTokenEncryptionAlgRSA_OAEPEncA128GCM() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-256", adminClient, testContext);
        testIdTokenSignatureAndEncryption(Algorithm.ES256, JWEConstants.RSA_OAEP, JWEConstants.A128GCM);
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
            OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
            String code = response.getCode();
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

            // parse JWE and JOSE Header
            String jweStr = tokenResponse.getIdToken();
            String[] parts = jweStr.split("\\.");
            Assert.assertEquals(parts.length, 5);

            // get decryption key
            // not publickey , use privateKey
            Map<String, String> keyPair = oidcClientEndpointsResource.getKeysAsPem();
            PrivateKey decryptionKEK = PemUtils.decodePrivateKey(keyPair.get("privateKey"));

            // verify and decrypt JWE
            JWEAlgorithmProvider algorithmProvider = getJweAlgorithmProvider(algAlgorithm);
            JWEEncryptionProvider encryptionProvider = getJweEncryptionProvider(encAlgorithm);
            byte[] decodedString = TokenUtil.jweKeyEncryptionVerifyAndDecode(decryptionKEK, jweStr, algorithmProvider, encryptionProvider);
            String idTokenString = new String(decodedString, "UTF-8");

            // verify JWS
            IDToken idToken = oauth.verifyIDToken(idTokenString);
            Assert.assertEquals("test-user@localhost", idToken.getPreferredUsername());
            Assert.assertEquals("test-app", idToken.getIssuedFor());
        } catch (JWEException | UnsupportedEncodingException e) {
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
        JWEAlgorithmProvider jweAlgorithmProvider = null;
        if (JWEConstants.RSA1_5.equals(algAlgorithm) || JWEConstants.RSA_OAEP.equals(algAlgorithm) ) {
            jweAlgorithmProvider = new RsaCekManagementProvider(null, algAlgorithm).jweAlgorithmProvider();
        }
        return jweAlgorithmProvider;
    }
    private JWEEncryptionProvider getJweEncryptionProvider(String encAlgorithm) {
        JWEEncryptionProvider jweEncryptionProvider = null;
        if (JWEConstants.A128CBC_HS256.equals(encAlgorithm)) {
            jweEncryptionProvider = new AesCbcHmacShaContentEncryptionProvider(null, encAlgorithm).jweEncryptionProvider();
        } else if (JWEConstants.A128GCM.equals(encAlgorithm)) {
            jweEncryptionProvider = new AesGcmContentEncryptionProvider(null, encAlgorithm).jweEncryptionProvider();
        }
        return jweEncryptionProvider;
    }

    @Test
    @UncaughtServerErrorExpected
    public void testIdTokenEncryptionWithoutEncryptionKEK() {
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
            OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
            AccessTokenResponse atr = oauth.doAccessTokenRequest(response.getCode(), "password");
            Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, atr.getError());
            Assert.assertEquals("can not get encryption KEK", atr.getErrorDescription());

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

