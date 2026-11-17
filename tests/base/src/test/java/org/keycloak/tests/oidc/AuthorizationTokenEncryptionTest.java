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
package org.keycloak.tests.oidc;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Map;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.JwksProvider;
import org.keycloak.testframework.oauth.annotations.InjectJwksProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AuthorizationTokenEncryptionTest extends AbstractOIDCScopeTest{
    private static final String USERNAME = "basic-user";
    private static final String PASSWORD = "password";
    private static final String STATE = "OpenIdConnect.AuthenticationProperties=2302984sdlk";

    @InjectRealm(config = BasicRealmWithUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectJwksProvider
    JwksProvider jwksProvider;

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA128CBC_HS256() {
        registerEcKeyProvider("P-256",managedRealm);
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
        testAuthorizationTokenSignatureAndEncryption(Algorithm.EdDSA, JWEConstants.RSA1_5, JWEConstants.A192GCM);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA256GCM() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.RS256, JWEConstants.RSA1_5, JWEConstants.A256GCM);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA128CBC_HS256() {
        registerEcKeyProvider("P-521",managedRealm);
        testAuthorizationTokenSignatureAndEncryption(Algorithm.ES512, JWEConstants.RSA_OAEP, JWEConstants.A128CBC_HS256);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA192CBC_HS384() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA_OAEP, JWEConstants.A192CBC_HS384);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEPEncA256CBC_HS512() {
        testAuthorizationTokenSignatureAndEncryption(Algorithm.EdDSA, JWEConstants.RSA_OAEP, JWEConstants.A256CBC_HS512);
    }

    @Test
    public void testAuthorizationEncryptionAlgRSA_OAEP256EncA128CBC_HS256() {
        registerEcKeyProvider("P-521",managedRealm);
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
        registerEcKeyProvider("P-256",managedRealm);
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

    @Test
    public void testAuthorizationEncryptionWithoutEncryptionKEK() {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            // generate and register signing/verifying key onto client, not encryption key
            jwksProvider.generateKeys(Algorithm.RS256);

            clientResource = oauth.clientResource();
            clientRep = clientResource.toRepresentation();
            // set signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationSignedResponseAlg(Algorithm.RS256);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseAlg(JWEConstants.RSA1_5);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseEnc(JWEConstants.A128CBC_HS256);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = jwksProvider.getUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);

            // open login form with state and responseMode=jwt
            oauth.responseMode("jwt");
            oauth.loginForm().state(STATE).open();

            // authenticate on login page
            authenticatePassword(USERNAME,PASSWORD);

            errorPage.assertCurrent();
        } finally {
            // Revert
            clientResource = oauth.clientResource();
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

    private void testAuthorizationTokenSignatureAndEncryption(String sigAlgorithm, String algAlgorithm, String encAlgorithm) {
        ClientResource clientResource;
        ClientRepresentation clientRep;
        try {
            // generate and register encryption key onto client via JwksProvider
            Map<String, String> keyPair = jwksProvider.generateKeys(algAlgorithm);

            clientResource = oauth.clientResource();
            clientRep = clientResource.toRepresentation();
            // set authorization response signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationSignedResponseAlg(sigAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseAlg(algAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationEncryptedResponseEnc(encAlgorithm);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = jwksProvider.getUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);

            // open login form with state and responseMode=jwt
            oauth.responseMode("jwt");
            oauth.loginForm().state(STATE).open();

            // authenticate on login page
            authenticatePassword(USERNAME,PASSWORD);

            AuthorizationEndpointResponse response = oauth.parseLoginResponse();
            response.isError();

            // parse JWE and JOSE Header
            String jweStr = response.getResponse();
            String[] parts = jweStr.split("\\.");
            Assertions.assertEquals(5, parts.length);

            // get decryption key
            PrivateKey decryptionKEK = PemUtils.decodePrivateKey(keyPair.get(JwksProvider.PRIVATE_KEY));

            // verify and decrypt JWE
            JWEAlgorithmProvider algorithmProvider = getJweAlgorithmProvider(algAlgorithm);
            JWEEncryptionProvider encryptionProvider = getJweEncryptionProvider(encAlgorithm);
            byte[] decodedString = TokenUtil.jweKeyEncryptionVerifyAndDecode(decryptionKEK, jweStr, algorithmProvider, encryptionProvider);
            String authorizationTokenString = new String(decodedString, StandardCharsets.UTF_8);

            // a nested JWT (signed and encrypted JWT) needs to set "JWT" to its JOSE Header's "cty" field
            JWEHeader jweHeader = (JWEHeader) getHeader(parts[0]);
            Assertions.assertEquals("JWT", jweHeader.getContentType());

            // verify JWS
            AuthorizationResponseToken authorizationToken = oauth.verifyAuthorizationResponseToken(authorizationTokenString);
            Assertions.assertEquals(oauth.getClientId(), authorizationToken.getAudience()[0]);
            Assertions.assertEquals(STATE, authorizationToken.getOtherClaims().get("state"));
            Assertions.assertNotNull(authorizationToken.getOtherClaims().get("code"));
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
