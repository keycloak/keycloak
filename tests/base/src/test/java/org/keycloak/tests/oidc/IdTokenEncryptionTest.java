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
package org.keycloak.tests.oidc;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Map;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.JwksProvider;
import org.keycloak.testframework.oauth.annotations.InjectJwksProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class IdTokenEncryptionTest extends AbstractOIDCScopeTest{
    private static final String USERNAME = "basic-user";
    private static final String PASSWORD = "password";

    @InjectRealm(config = BasicRealmWithUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectJwksProvider
    JwksProvider jwksProvider;

    @Test
    public void testIdTokenEncryptionAlgRSA1_5EncA128CBC_HS256() {
        registerEcKeyProvider("P-256",managedRealm);
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
        registerEcKeyProvider("P-521",managedRealm);
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
        registerEcKeyProvider("P-521",managedRealm);
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
        registerEcKeyProvider("P-256",managedRealm);
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

    @Test
    public void testIdTokenEncryptionWithoutEncryptionKEK() {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            // generate and register signing/verifying key onto client, not encryption key
            jwksProvider.generateKeys(Algorithm.RS256);

            clientResource = oauth.clientResource();
            clientRep = clientResource.toRepresentation();
            // set id token signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenSignedResponseAlg(Algorithm.RS256);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseAlg(JWEConstants.RSA1_5);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseEnc(JWEConstants.A128CBC_HS256);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksProvider.getUri());
            clientResource.update(clientRep);

            // open login form
            oauth.loginForm().open();

            // authenticate on login page
            authenticatePassword(USERNAME,PASSWORD);

            // wait for response and get id token but should fail
            AuthorizationEndpointResponse response = oauth.parseLoginResponse();
            AccessTokenResponse atr = oauth.doAccessTokenRequest(response.getCode());
            Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, atr.getError());
            Assertions.assertEquals("can not get encryption KEK", atr.getErrorDescription());

            // get id token but failed with client_credentials grant type
            oauth.scope("openid");
            AccessTokenResponse responseClientCredentials = oauth.client(clientRep.getClientId(), clientRep.getSecret()).doClientCredentialsGrantAccessTokenRequest();
            Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, responseClientCredentials.getError());
            Assertions.assertEquals("can not get encryption KEK", responseClientCredentials.getErrorDescription());
        } finally {
            clientResource = oauth.clientResource();
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

    private void testIdTokenSignatureAndEncryption(String sigAlgorithm, String algAlgorithm, String encAlgorithm) {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            // generate and register encryption key onto client via JwksProvider
            Map<String, String> keyPair = jwksProvider.generateKeys(algAlgorithm);

            clientResource = oauth.clientResource();
            clientRep = clientResource.toRepresentation();
            // set id token signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenSignedResponseAlg(sigAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseAlg(algAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenEncryptedResponseEnc(encAlgorithm);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksProvider.getUri());
            clientResource.update(clientRep);

            // open login form
            oauth.loginForm().open();

            // authenticate on login page
            authenticatePassword(USERNAME,PASSWORD);


            // wait for response and get authorization code
            AuthorizationEndpointResponse response = oauth.parseLoginResponse();
            String code = response.getCode();

            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

            // parse JWE and JOSE Header
            String jweStr = tokenResponse.getIdToken();
            String[] parts = jweStr.split("\\.");
            Assertions.assertEquals(5, parts.length);

            // get decryption key
            PrivateKey decryptionKEK = PemUtils.decodePrivateKey(keyPair.get(JwksProvider.PRIVATE_KEY));

            // a nested JWT (signed and encrypted JWT) needs to set "JWT" to its JOSE Header's "cty" field
            JWEHeader jweHeader = (JWEHeader) getHeader(parts[0]);
            Assertions.assertEquals("JWT", jweHeader.getContentType());

            // verify and decrypt JWE
            if (encAlgorithm == null) encAlgorithm = JWEConstants.A128CBC_HS256;
            JWEAlgorithmProvider algorithmProvider = getJweAlgorithmProvider(algAlgorithm);
            JWEEncryptionProvider encryptionProvider = getJweEncryptionProvider(encAlgorithm);
            byte[] decodedString = TokenUtil.jweKeyEncryptionVerifyAndDecode(decryptionKEK, jweStr, algorithmProvider, encryptionProvider);
            String idTokenString = new String(decodedString, StandardCharsets.UTF_8);

            // verify JWS
            IDToken idToken = oauth.verifyIDToken(idTokenString);
            Assertions.assertEquals(USERNAME, idToken.getPreferredUsername());
            Assertions.assertEquals(oauth.getClientId(), idToken.getIssuedFor());
        } catch (JWEException e) {
            Assertions.fail(e);
        } finally {
            clientResource = oauth.clientResource();
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
}
