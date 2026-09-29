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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MultivaluedHashMap;
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
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.JwksProvider;
import org.keycloak.testframework.oauth.annotations.InjectJwksProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AuthorizationTokenEncryptionTest extends AbstractOIDCScopeTest{

    private static final String CLIENT_ID = "test-app";
    private static final String USERNAME = "test-user@localhost";
    private static final String PASSWORD = "password";
    private static final String STATE = "OpenIdConnect.AuthenticationProperties=2302984sdlk";

    @InjectRealm(config = AuthorizationTokenEncryptionRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;
    
    @InjectJwksProvider
    JwksProvider jwksProvider;

    @Test
    public void testAuthorizationEncryptionAlgRSA1_5EncA128CBC_HS256() {
        registerEcKeyProvider("P-256");
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
        registerEcKeyProvider("P-521");
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
        registerEcKeyProvider("P-521");
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
        registerEcKeyProvider("P-256");
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
            // generate and register encryption key onto client via JwksProvider
            Map<String, String> keyPair = jwksProvider.generateKeys(algAlgorithm);

            clientResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), CLIENT_ID);
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
            authenticatePassword();

            // wait for response query parameter in URL and parse response
            driver.waiting().waitForOAuthCallback(d -> d.getCurrentUrl().contains(OAuth2Constants.RESPONSE + "=") || d.getCurrentUrl().contains(OAuth2Constants.ERROR + "="));
            AuthorizationEndpointResponse response = new AuthorizationEndpointResponse(oauth);

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
            Assertions.assertEquals(CLIENT_ID, authorizationToken.getAudience()[0]);
            Assertions.assertEquals(STATE, authorizationToken.getOtherClaims().get("state"));
            Assertions.assertNotNull(authorizationToken.getOtherClaims().get("code"));
        } catch (JWEException e) {
            Assertions.fail(e);
        } finally {
            clientResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), CLIENT_ID);
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

    private void authenticatePassword() {
        loginPage.assertCurrent();
        loginPage.fillLogin(AuthorizationTokenEncryptionTest.USERNAME, AuthorizationTokenEncryptionTest.PASSWORD);
        loginPage.submit();
    }

    private void registerEcKeyProvider(String ecCurve) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("ecdsa-" + ecCurve);
        rep.setParentId(managedRealm.admin().toRepresentation().getId());
        rep.setProviderId(GeneratedEcdsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, Long.toString(System.currentTimeMillis()));
        rep.getConfig().putSingle("active", "true");
        rep.getConfig().putSingle("enabled", "true");
        rep.getConfig().putSingle(GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY, ecCurve);

        try (Response response = managedRealm.admin().components().add(rep)) {
            String id = ApiUtil.getCreatedId(response);
            managedRealm.cleanup().add(r -> r.components().component(id).remove());
        }
    }

    private JWEAlgorithmProvider getJweAlgorithmProvider(String algAlgorithm) {
        return new RsaCekManagementProvider(null, algAlgorithm).jweAlgorithmProvider();
    }

    private JWEEncryptionProvider getJweEncryptionProvider(String encAlgorithm) {
        JWEEncryptionProvider jweEncryptionProvider = null;
        switch (encAlgorithm) {
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
            default:
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
    public void testAuthorizationEncryptionWithoutEncryptionKEK() {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            // generate and register signing/verifying key onto client, not encryption key
            jwksProvider.generateKeys(Algorithm.RS256);

            clientResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), CLIENT_ID);
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
            authenticatePassword();

            errorPage.assertCurrent();
            Assertions.assertTrue(driver.page().getPageSource().contains("Unexpected error when handling authentication request to identity provider."));
        } finally {
            // Revert
            clientResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), CLIENT_ID);
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

    private static class AuthorizationTokenEncryptionRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .users(UserBuilder.create("test-user")
                            .email(USERNAME)
                            .username(USERNAME)
                            .firstName("test")
                            .lastName("user")
                            .password(PASSWORD));
        }
    }

}
