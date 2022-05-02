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

package org.keycloak.testsuite.keys;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
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
import org.keycloak.keys.KeyProvider;
import org.keycloak.keys.ImportedRsaKeyProviderFactory;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserInfoClientUtil;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeyRotationTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);

        ClientRepresentation confApp = KeycloakModelUtils.createClient(realm, "confidential-cli");
        confApp.setSecret("secret1");
        confApp.setServiceAccountsEnabled(Boolean.TRUE);
    }

    @Test
    public void testIdentityCookie() throws Exception {
        // Create keys #1
        createKeys1();

        // Login with keys #1
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Create keys #2
        createKeys2();

        // Login again with cookie signed with old keys
        appPage.open();
        oauth.openLoginForm();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Drop key #1
        dropKeys1();

        // Login again with key #1 dropped - should pass as cookie should be refreshed
        appPage.open();
        oauth.openLoginForm();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Drop key #2
        dropKeys2();

        // Login again with key #2 dropped - should fail as cookie hasn't been refreshed
        appPage.open();
        oauth.openLoginForm();
        assertTrue(loginPage.isCurrent());
    }

    @Test
    public void testTokens() throws Exception {
        // Create keys #1
        Map<String, String> keys1 = createKeys1();

        // Get token with keys #1
        oauth.doLogin("test-user@localhost", "password");
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get("code"), "password");
        assertEquals(200, response.getStatusCode());
        assertTokenKid(keys1.get(Algorithm.RS256), response.getAccessToken());
        assertTokenKid(keys1.get(Algorithm.HS256), response.getRefreshToken());

        // Create client with keys #1
        ClientInitialAccessCreatePresentation initialToken = new ClientInitialAccessCreatePresentation();
        initialToken.setCount(100);
        initialToken.setExpiration(0);
        ClientInitialAccessPresentation accessRep = adminClient.realm("test").clientInitialAccess().create(initialToken);
        String initialAccessToken = accessRep.getToken();

        ClientRegistration reg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", "test").build();
        reg.auth(Auth.token(initialAccessToken));
        ClientRepresentation clientRep = reg.create(ClientBuilder.create().clientId("test").build());

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
        assertNotEquals(keys1.get(Algorithm.HS256), keys2.get(Algorithm.HS512));

                // Refresh token with keys #2
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        assertEquals(200, response.getStatusCode());
        assertTokenKid(keys2.get(Algorithm.RS256), response.getAccessToken());
        assertTokenKid(keys2.get(Algorithm.HS256), response.getRefreshToken());

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
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");

        assertTokenKid(keys2.get(Algorithm.RS256), response.getAccessToken());
        assertTokenKid(keys2.get(Algorithm.HS256), response.getRefreshToken());

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
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
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
    public void rotateKeys() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            String activeKid = adminClient.realm("test").keys().getKeyMetadata().getActive().get(Algorithm.RS256);

            // Rotate public keys on the parent broker
            String realmId = adminClient.realm("test").toRepresentation().getId();
            ComponentRepresentation keys = new ComponentRepresentation();
            keys.setName("generated" + i);
            keys.setProviderType(KeyProvider.class.getName());
            keys.setProviderId("rsa-generated");
            keys.setParentId(realmId);
            keys.setConfig(new MultivaluedHashMap<>());
            keys.getConfig().putSingle("priority", "1000" + i);
            Response response = adminClient.realm("test").components().add(keys);
            assertEquals(201, response.getStatus());
            String newId = ApiUtil.getCreatedId(response);
            getCleanup().addComponentId(newId);
            response.close();

            String updatedActiveKid = adminClient.realm("test").keys().getKeyMetadata().getActive().get(Algorithm.RS256);
            assertNotEquals(activeKid, updatedActiveKid);
        }
    }


    private void assertTokenKid(String expectedKid, String token) throws JWSInputException {
        assertEquals(expectedKid, new JWSInput(token).getHeader().getKeyId());
    }

    private Map<String, String> createKeys1() throws Exception {
        return createKeys("1000");
    }

    private Map<String, String> createKeys2() throws Exception {
        return createKeys("2000");
    }

    private Map<String, String> createKeys(String priority) throws Exception {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(1024);
        String privateKeyPem = PemUtils.encodeKey(keyPair.getPrivate());
        PublicKey publicKey = keyPair.getPublic();

        String testRealmId = adminClient.realm("test").toRepresentation().getId();
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("mycomponent");
        rep.setParentId(testRealmId);
        rep.setProviderId(ImportedRsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());

        org.keycloak.common.util.MultivaluedHashMap config = new org.keycloak.common.util.MultivaluedHashMap();
        config.addFirst("priority", priority);
        config.addFirst(Attributes.PRIVATE_KEY_KEY, privateKeyPem);
        rep.setConfig(config);

        Response response = adminClient.realm("test").components().add(rep);
        response.close();

        rep = new ComponentRepresentation();
        rep.setName("mycomponent2");
        rep.setParentId(testRealmId);
        rep.setProviderId(GeneratedHmacKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());

        config = new org.keycloak.common.util.MultivaluedHashMap();
        config.addFirst("priority", priority);
        rep.setConfig(config);

        response = adminClient.realm("test").components().add(rep);
        response.close();

        return realmsResouce().realm("test").keys().getKeyMetadata().getActive();
    }

    private void dropKeys1() {
        dropKeys("1000");
    }

    private void dropKeys2() {
        dropKeys("2000");
    }

    private void dropKeys(String priority) {
        int r = 0;
        String parentId = adminClient.realm("test").toRepresentation().getId();
        for (ComponentRepresentation c : adminClient.realm("test").components().query(parentId, KeyProvider.class.getName())) {
            if (c.getConfig().getFirst("priority").equals(priority)) {
                adminClient.realm("test").components().component(c.getId()).remove();
                r++;
            }
        }
        if (r != 2) {
            throw new RuntimeException("Failed to find keys1");
        }
    }

    private void assertUserInfo(String token, int expectedStatus) {
        try (Response userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(AdminClientUtil.createResteasyClient(), token)) {
            assertEquals(expectedStatus, userInfoResponse.getStatus());
        }
    }

    private void assertTokenIntrospection(String token, boolean expectActive) {
        try {
            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", token);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(tokenResponse);
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

