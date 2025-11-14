/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.keys;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedHmacKeyProviderFactory;
import org.keycloak.keys.ImportedRsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.InjectCryptoHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.crypto.CryptoHelper;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class KeyRotationTest {

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectCryptoHelper
    CryptoHelper cryptoHelper;

    @Test
    public void testIdentityCookie() {
        // Create keys #1
        createKeys1();

        // Login with keys #1
        AuthorizationEndpointResponse response = oauth.doLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);
        assertTrue(response.isRedirected());

        // Create keys #2
        createKeys2();

        // Login again with cookie signed with old keys
        oauth.openLoginForm();
        assertTrue(oauth.parseLoginResponse().isRedirected());

        // Drop key #1
        dropKeys1();

        // Login again with key #1 dropped - should pass as cookie should be refreshed
        oauth.openLoginForm();
        assertTrue(oauth.parseLoginResponse().isRedirected());

        // Drop key #2
        dropKeys2();

        // Login again with key #2 dropped - should fail as cookie hasn't been refreshed
        oauth.openLoginForm();
        assertFalse(oauth.parseLoginResponse().isRedirected());
    }

    @Test
    public void testTokens() throws Exception {
        // Create keys #1
        Map<String, String> keys1 = createKeys1();

        // Get token with keys #1
        oauth.doLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);
        AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(200, response.getStatusCode());
        assertTokenKid(keys1.get(Algorithm.RS256), response.getAccessToken());
        assertTokenKid(keys1.get(Constants.INTERNAL_SIGNATURE_ALGORITHM), response.getRefreshToken());

        // Create client with keys #1
        ClientInitialAccessCreatePresentation initialToken = new ClientInitialAccessCreatePresentation();
        initialToken.setCount(100);
        initialToken.setExpiration(0);
        ClientInitialAccessPresentation accessRep = realm.admin().clientInitialAccess().create(initialToken);
        String initialAccessToken = accessRep.getToken();

        ClientRegistration reg = oauth.clientRegistration();
        reg.auth(Auth.token(initialAccessToken));
        ClientRepresentation clientRep = reg.create(ClientConfigBuilder.create().clientId("test").build());

        // Userinfo with keys #1
        assertUserInfo(response.getAccessToken(), 200);

        // Token introspection with keys #1
        assertTokenIntrospection(response.getAccessToken(), true);

        // Get client with keys #1 - registration access token should not have changed
        ClientRepresentation clientRep2 = reg.auth(Auth.token(clientRep.getRegistrationAccessToken())).get("test");
        assertEquals(clientRep.getRegistrationAccessToken(), clientRep2.getRegistrationAccessToken());

        // Create keys #2
        Map<String, String> keys2 = createKeys2();

        assertNotEquals(keys1.get(Algorithm.RS256), keys2.get(Algorithm.RS256));
        assertNotEquals(keys1.get(Constants.INTERNAL_SIGNATURE_ALGORITHM), keys2.get(Algorithm.HS512));

                // Refresh token with keys #2
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, response.getStatusCode());
        assertTokenKid(keys2.get(Algorithm.RS256), response.getAccessToken());
        assertTokenKid(keys2.get(Constants.INTERNAL_SIGNATURE_ALGORITHM), response.getRefreshToken());

        // Userinfo with keys #2
        assertUserInfo(response.getAccessToken(), 200);

        // Token introspection with keys #2
        assertTokenIntrospection(response.getAccessToken(), true);

        // Get client with keys #2 - registration access token should be changed
        ClientRepresentation clientRep3 = reg.auth(Auth.token(clientRep.getRegistrationAccessToken())).get("test");
        assertNotEquals(clientRep.getRegistrationAccessToken(), clientRep3.getRegistrationAccessToken());

        // Drop key #1
        dropKeys1();

        // Refresh token with keys #1 dropped - should pass as refresh token should be signed with key #2
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());

        assertTokenKid(keys2.get(Algorithm.RS256), response.getAccessToken());
        assertTokenKid(keys2.get(Constants.INTERNAL_SIGNATURE_ALGORITHM), response.getRefreshToken());

        // Userinfo with keys #1 dropped
        assertUserInfo(response.getAccessToken(), 200);

        // Token introspection with keys #1 dropped
        assertTokenIntrospection(response.getAccessToken(), true);

        // Get client with keys #1 - should fail
        try {
            reg.auth(Auth.token(clientRep.getRegistrationAccessToken())).get("test");
            fail("Expected to fail");
        } catch (ClientRegistrationException e) {
        }

        // Get client with keys #2 - should succeed
        ClientRepresentation clientRep4 = reg.auth(Auth.token(clientRep3.getRegistrationAccessToken())).get("test");
        assertNotEquals(clientRep2.getRegistrationAccessToken(), clientRep4.getRegistrationAccessToken());

        // Drop key #2
        dropKeys2();

        // Userinfo with keys #2 dropped
        assertUserInfo(response.getAccessToken(), 401);

        // Token introspection with keys #2 dropped
        assertTokenIntrospection(response.getAccessToken(), false);

        // Refresh token with keys #2 dropped - should fail as refresh token is signed with key #2
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(400, response.getStatusCode());
        assertEquals("Invalid refresh token", response.getErrorDescription());
    }

    @Test
    public void providerOrder() throws Exception {
        Map<String, String> keys1 = createKeys1();
        Map<String, String> keys2 = createKeys2();

        assertNotEquals(keys1.get(Algorithm.RS256), keys2.get(Algorithm.RS256));
        assertNotEquals(keys1.get(Algorithm.HS256), keys2.get(Algorithm.HS512));

        dropKeys1();
        dropKeys2();
    }

    @Test
    public void rotateKeys() {
        realm.dirty();
        for (int i = 0; i < 10; i++) {
            String activeKid = realm.admin().keys().getKeyMetadata().getActive().get(Algorithm.RS256);

            // Rotate public keys on the parent broker
            String realmId = realm.getId();
            ComponentRepresentation keys = new ComponentRepresentation();
            keys.setName("generated" + i);
            keys.setProviderType(KeyProvider.class.getName());
            keys.setProviderId("rsa-generated");
            keys.setParentId(realmId);
            keys.setConfig(new MultivaluedHashMap<>());
            keys.getConfig().putSingle("priority", "1000" + i);
            Response response = realm.admin().components().add(keys);
            assertEquals(201, response.getStatus());
            String newId = ApiUtil.getCreatedId(response);
            response.close();

            String updatedActiveKid = realm.admin().keys().getKeyMetadata().getActive().get(Algorithm.RS256);
            assertNotEquals(activeKid, updatedActiveKid);
        }
    }


    private void assertTokenKid(String expectedKid, String token) throws JWSInputException {
        assertEquals(expectedKid, new JWSInput(token).getHeader().getKeyId());
    }

    private Map<String, String> createKeys1() {
        return createKeys("1000");
    }

    private Map<String, String> createKeys2() {
        return createKeys("2000");
    }

    private Map<String, String> createKeys(String priority) {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(Integer.parseInt(cryptoHelper.getExpectedSupportedRsaKeySizes()[0]));
        String privateKeyPem = PemUtils.encodeKey(keyPair.getPrivate());
        PublicKey publicKey = keyPair.getPublic();

        String testRealmId = realm.getId();
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("mycomponent");
        rep.setParentId(testRealmId);
        rep.setProviderId(ImportedRsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());

        org.keycloak.common.util.MultivaluedHashMap<String, String> config = new org.keycloak.common.util.MultivaluedHashMap<>();
        config.addFirst("priority", priority);
        config.addFirst(Attributes.PRIVATE_KEY_KEY, privateKeyPem);
        rep.setConfig(config);

        Response response = realm.admin().components().add(rep);
        response.close();

        rep = new ComponentRepresentation();
        rep.setName("mycomponent2");
        rep.setParentId(testRealmId);
        rep.setProviderId(GeneratedHmacKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());

        config = new org.keycloak.common.util.MultivaluedHashMap<>();
        config.addFirst(Attributes.PRIORITY_KEY, priority);
        config.addFirst(Attributes.ALGORITHM_KEY, Constants.INTERNAL_SIGNATURE_ALGORITHM);
        rep.setConfig(config);

        response = realm.admin().components().add(rep);
        response.close();

        return realm.admin().keys().getKeyMetadata().getActive();
    }

    private void dropKeys1() {
        dropKeys("1000");
    }

    private void dropKeys2() {
        dropKeys("2000");
    }

    private void dropKeys(String priority) {
        int r = 0;
        String parentId = realm.getId();
        for (ComponentRepresentation c : realm.admin().components().query(parentId, KeyProvider.class.getName())) {
            if (c.getConfig().getFirst("priority").equals(priority)) {
                realm.admin().components().component(c.getId()).remove();
                r++;
            }
        }
        if (r != 2) {
            throw new RuntimeException("Failed to find keys1");
        }
    }

    private void assertUserInfo(String token, int expectedStatus) {
        UserInfoResponse response = oauth.doUserInfoRequest(token);
        assertEquals(expectedStatus, response.getStatusCode());
    }

    private void assertTokenIntrospection(String token, boolean expectActive) {
        try {
            JsonNode jsonNode = oauth.doIntrospectionAccessTokenRequest(token).asJsonNode();
            assertEquals(expectActive, jsonNode.get("active").asBoolean());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private class ActiveKeys {

        private String rsaKid;
        private String hsKid;

    }

}
