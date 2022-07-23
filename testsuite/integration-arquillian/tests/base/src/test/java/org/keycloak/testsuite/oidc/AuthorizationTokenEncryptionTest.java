/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
import org.junit.Rule;
import org.junit.Test;
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
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.util.Map;

public class AuthorizationTokenEncryptionTest extends AbstractTestRealmKeycloakTest {

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

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA128CBC_HS256() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-256", adminClient, testContext);
        testAuthorizationTokenSignatureAndEncryption(Algorithm.ES256, JWEConstants.RSA1_5, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA192CBC_HS384() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA1_5, JWEConstants.A192CBC_HS384);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA256CBC_HS512() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS384, JWEConstants.RSA1_5, JWEConstants.A256CBC_HS512);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA128GCM() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.RS384, JWEConstants.RSA1_5, JWEConstants.A128GCM);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA192GCM() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.RS512, JWEConstants.RSA1_5, JWEConstants.A192GCM);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA256GCM() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.RS256, JWEConstants.RSA1_5, JWEConstants.A256GCM);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA128CBC_HS256() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-521", adminClient, testContext);
        testAuthorizationTokenSignatureAndEncryption(Algorithm.ES512, JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA192CBC_HS384() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA_OAEP, JWEConstants.A192CBC_HS384);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA256CBC_HS512() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS512, JWEConstants.RSA_OAEP, JWEConstants.A256CBC_HS512);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEP256EncA128CBC_HS256() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-521", adminClient, testContext);
        testAuthorizationTokenSignatureAndEncryption(Algorithm.ES512, JWEConstants.RSA_OAEP_256, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEP256EncA192CBC_HS384() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA_OAEP_256, JWEConstants.A192CBC_HS384);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEP256EncA256CBC_HS512() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS512, JWEConstants.RSA_OAEP_256, JWEConstants.A256CBC_HS512);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA128GCM() {
        // add key provider explicitly though DefaultKeyManager create fallback key provider if not exist
        TokenSignatureUtil.registerKeyProvider("P-256", adminClient, testContext);
        testAuthorizationTokenSignatureAndEncryption(Algorithm.ES256, JWEConstants.RSA_OAEP, JWEConstants.A128GCM);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA192GCM() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS384, JWEConstants.RSA_OAEP, JWEConstants.A192GCM);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA256GCM() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS512, JWEConstants.RSA_OAEP, JWEConstants.A256GCM);
    }

    private void testAuthorizationTokenSignatureAndEncryption(String sigAlgorithm, String algAlgorithm, String encAlgorithm) {
        ClientResource clientResource;
        ClientRepresentation clientRep;
        try {
            // generate and register encryption key onto client
            TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
            oidcClientEndpointsResource.generateKeys(algAlgorithm);

            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            // set authorization response signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationSignedResponseAlg(sigAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseAlg(algAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseEnc(encAlgorithm);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);

            // get authorization response
            oauth.responseMode("jwt");
            oauth.stateParamHardcoded("OpenIdConnect.AuthenticationProperties=2302984sdlk");
            OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

            // parse JWE and JOSE Header
            String jweStr = response.getResponse();
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
            String authorizationTokenString = new String(decodedString, "UTF-8");

            // a nested JWT (signed and encrypted JWT) needs to set "JWT" to its JOSE Header's "cty" field
            JWEHeader jweHeader = (JWEHeader) getHeader(parts[0]);
            Assert.assertEquals("JWT", jweHeader.getContentType());

            // verify JWS
            AuthorizationResponseToken authorizationToken = oauth.verifyAuthorizationResponseToken(authorizationTokenString);
            Assert.assertEquals("test-app", authorizationToken.getAudience()[0]);
            Assert.assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", authorizationToken.getOtherClaims().get("state"));
            Assert.assertNotNull(authorizationToken.getOtherClaims().get("code"));
        } catch (JWEException | UnsupportedEncodingException e) {
            Assert.fail();
        } finally {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            // revert id token signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationSignedResponseAlg(Algorithm.RS256);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseAlg(null);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseEnc(null);
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
    public void testAuthorizationEncryptionWithoutEncryptionKEK() throws MalformedURLException, URISyntaxException {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            // generate and register signing/verifying key onto client, not encryption key
            TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
            oidcClientEndpointsResource.generateKeys(Algorithm.RS256);

            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            // set id token signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationSignedResponseAlg(Algorithm.RS256);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseAlg(JWEConstants.RSA1_5);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseEnc(JWEConstants.A128CBC_HS256);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);
 
            // get authorization response but failed
            oauth.responseMode("jwt");
            oauth.stateParamHardcoded("OpenIdConnect.AuthenticationProperties=2302984sdlk");

            OAuthClient.AuthorizationEndpointResponse errorResponse =  oauth.doLogin("test-user@localhost", "password");

            System.out.println(driver.getPageSource().contains("Unexpected error when handling authentication request to identity provider."));

        } finally {
            // Revert
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationSignedResponseAlg(Algorithm.RS256);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseAlg(null);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseEnc(null);
            // Revert jwks_url settings
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(false);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(null);
            clientResource.update(clientRep);
        }
    }

}

