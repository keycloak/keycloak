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

package org.keycloak.testsuite.oidc;

import org.junit.After;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.auth.page.login.Login;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:daniel.lekberg@redpill-linpro.com">Daniel Lekberg</a>
 */
public class UserClientRoleMappingMapperTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(
                getClass().getResourceAsStream("/testrealm.json"),
                RealmRepresentation.class
        );
        testRealms.add(realm);
    }

    @After
    public void deprovisionUserClientRoleMappingMapper() {
        ClientManager
                .realm(adminClient.realm("test"))
                .clientId("test-app")
                .removeProtocolMapper("UserClientRoleMappingMapperTest");
    }

    public void provisionUserClientRoleMappingMapper(String prefix) {
        ProtocolMapperRepresentation userClientRoleMappingMapper = new ProtocolMapperRepresentation();
        userClientRoleMappingMapper.setName("UserClientRoleMappingMapperTest");
        userClientRoleMappingMapper.setProtocol(Login.OIDC);
        userClientRoleMappingMapper.setProtocolMapper("oidc-usermodel-client-role-mapper");
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("usermodel.clientRoleMapping.rolePrefix", prefix);
        configuration.put("introspection.token.claim", "true");
        configuration.put("multivalued", "true");
        configuration.put("userinfo.token.claim", "true");
        configuration.put("id.token.claim", "true");
        configuration.put("access.token.claim", "true");
        configuration.put("claim.name", "roles");
        configuration.put("jsonType.label", "String");
        userClientRoleMappingMapper.setConfig(configuration);
        ClientManager
                .realm(adminClient.realm("test"))
                .clientId("test-app")
                .fullScopeAllowed(false)
                .addProtocolMapper(userClientRoleMappingMapper);
    }

    @Test
    public void testUserClientRoleMappingMapperWithNullPrefix() {
        provisionUserClientRoleMappingMapper(null);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        assertNotNull(idToken);
        List<String> idTokenRoles = (List<String>) idToken.getOtherClaims().get("roles");
        assertEquals(List.of("customer-user"), idTokenRoles);
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertNotNull(accessToken);
        List<String> accessTokenRoles = (List<String>) accessToken.getOtherClaims().get("roles");
        assertEquals(List.of("customer-user"), accessTokenRoles);
    }

    @Test
    public void testUserClientRoleMappingMapperWithEmptyPrefix() {
        provisionUserClientRoleMappingMapper("");
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        assertNotNull(idToken);
        List<String> idTokenRoles = (List<String>) idToken.getOtherClaims().get("roles");
        assertEquals(List.of("customer-user"), idTokenRoles);
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertNotNull(accessToken);
        List<String> accessTokenRoles = (List<String>) accessToken.getOtherClaims().get("roles");
        assertEquals(List.of("customer-user"), accessTokenRoles);
    }

    @Test
    public void testUserClientRoleMappingMapperWithFixedPrefix() {
        provisionUserClientRoleMappingMapper("client_id::");
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        assertNotNull(idToken);
        List<String> idTokenRoles = (List<String>) idToken.getOtherClaims().get("roles");
        assertEquals(List.of("client_id::customer-user"), idTokenRoles);
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertNotNull(accessToken);
        List<String> accessTokenRoles = (List<String>) accessToken.getOtherClaims().get("roles");
        assertEquals(List.of("client_id::customer-user"), accessTokenRoles);
    }

    @Test
    public void testUserClientRoleMappingMapperWithClientIdPlaceholderInPrefix() {
        provisionUserClientRoleMappingMapper("${client_id}::");
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        assertNotNull(idToken);
        List<String> idTokenRoles = (List<String>) idToken.getOtherClaims().get("roles");
        assertEquals(List.of("test-app::customer-user"), idTokenRoles);
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertNotNull(accessToken);
        List<String> accessTokenRoles = (List<String>) accessToken.getOtherClaims().get("roles");
        assertEquals(List.of("test-app::customer-user"), accessTokenRoles);
    }
}
