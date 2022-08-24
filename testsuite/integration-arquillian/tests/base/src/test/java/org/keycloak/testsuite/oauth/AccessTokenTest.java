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
package org.keycloak.testsuite.oauth;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.UserManager;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.Assert.assertExpiration;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createRoleNameMapper;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccessTokenTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
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
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        UserBuilder user = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("no-permissions")
                .addRoles("user")
                .password("password");
        realm.getUsers().add(user.build());

        realm.getClients().stream().filter(clientRepresentation -> {

            return "test-app".equals(clientRepresentation.getClientId());

        }).forEach(clientRepresentation -> {

            clientRepresentation.setFullScopeAllowed(false);

        });

        testRealms.add(realm);

    }
    
    @Test
    public void loginFormUsernameOrEmailLabel() throws Exception {
        oauth.openLoginForm();
        
        assertEquals("Username or email", driver.findElement(By.xpath("//label[@for='username']")).getText());
    }

    @Test
    public void accessTokenRequest() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(response.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(1750), lessThanOrEqualTo(1800)));

        assertEquals("Bearer", response.getTokenType());

        String expectedKid = oauth.doCertsRequest("test").getKeys()[0].getKeyId();

        JWSHeader header = new JWSInput(response.getAccessToken()).getHeader();
        assertEquals("RS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertEquals(expectedKid, header.getKeyId());
        assertNull(header.getContentType());

        header = new JWSInput(response.getIdToken()).getHeader();
        assertEquals("RS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertEquals(expectedKid, header.getKeyId());
        assertNull(header.getContentType());

        header = new JWSInput(response.getRefreshToken()).getHeader();
        assertEquals("HS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), token.getSubject());
        assertNotEquals("test-user@localhost", token.getSubject());

        assertEquals(sessionId, token.getSessionState());

        JWSInput idToken = new JWSInput(response.getIdToken());
        ObjectMapper mapper = JsonSerialization.mapper;
        JsonParser parser = mapper.getFactory().createParser(idToken.readContentAsString());
        TreeNode treeNode = mapper.readTree(parser);
        String sid = ((TextNode) treeNode.get("sid")).asText();
        assertEquals(sessionId, sid);

        assertNull(token.getNbf());
        assertEquals(0, token.getNotBefore());

        assertNotNull(token.getIat());
        assertEquals(token.getIat().intValue(), token.getIssuedAt());

        assertNotNull(token.getExp());
        assertEquals(token.getExp().intValue(), token.getExpiration());

        assertEquals(1, token.getRealmAccess().getRoles().size());
        assertTrue(token.getRealmAccess().isUserInRole("user"));

        assertEquals(1, token.getResourceAccess(oauth.getClientId()).getRoles().size());
        assertTrue(token.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation event = events.expectCodeToToken(codeId, sessionId).assertEvent();
        assertEquals(token.getId(), event.getDetails().get(Details.TOKEN_ID));
        assertEquals(oauth.parseRefreshToken(response.getRefreshToken()).getId(), event.getDetails().get(Details.REFRESH_TOKEN_ID));
        assertEquals(sessionId, token.getSessionState());

    }

    @Test
    public void testTokenResponseUsingLowerCaseType() throws Exception {
        ClientsResource clients = realmsResouce().realm("test").clients();
        ClientRepresentation client = clients.findByClientId(oauth.getClientId()).get(0);

        OIDCAdvancedConfigWrapper.fromClientRepresentation(client).setUseLowerCaseInTokenResponse(true);

        clients.get(client.getId()).update(client);

        try {
            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

            assertEquals(TokenUtil.TOKEN_TYPE_BEARER.toLowerCase(), response.getTokenType());
        } finally {
            OIDCAdvancedConfigWrapper.fromClientRepresentation(client).setUseLowerCaseInTokenResponse(false);
            clients.get(client.getId()).update(client);
        }
    }

    // KEYCLOAK-3692
    @Test
    public void accessTokenWrongCode() throws Exception {
        oauth.openLoginForm();

        String actionURI = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());
        String loginPageCode = ActionURIUtils.parseQueryParamsFromActionURI(actionURI).get("code");

        oauth.fillLoginForm("test-user@localhost", "password");

        events.expectLogin().assertEvent();

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(loginPageCode, "password");

        assertEquals(400, response.getStatusCode());
        assertNull(response.getRefreshToken());
    }

    @Test
    public void accessTokenInvalidClientCredentials() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "invalid");
        assertEquals(401, response.getStatusCode());

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, loginEvent.getSessionId()).error("invalid_client_credentials").clearDetails().user((String) null).session((String) null);
        expectedEvent.assertEvent();
    }

    @Test
    public void accessTokenMissingClientCredentials() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        assertEquals(401, response.getStatusCode());

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, loginEvent.getSessionId()).error("invalid_client_credentials").clearDetails().user((String) null).session((String) null);
        expectedEvent.assertEvent();
    }

    @Test
    public void accessTokenInvalidRedirectUri() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        //@TODO This new and was necesssary to not mess up with other tests cases
        String redirectUri = oauth.getRedirectUri();

        oauth.redirectUri("http://invalid");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        assertEquals("Incorrect redirect_uri", response.getErrorDescription());

        events.expectCodeToToken(codeId, loginEvent.getSessionId()).error("invalid_code")
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_TYPE)
                .assertEvent();

        //@TODO Reset back to the original URI. Maybe we should have something to reset to the original state at OAuthClient
        oauth.redirectUri(redirectUri);

    }

    @Test
    public void accessTokenUserSessionExpired() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String sessionId = loginEvent.getSessionId();


        testingClient.testing().removeUserSession("test", sessionId);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectCodeToToken(codeId, sessionId)
                .removeDetail(Details.TOKEN_ID)
                .user((String) null)
                .removeDetail(Details.REFRESH_TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_TYPE)
                .error(Errors.INVALID_CODE).assertEvent();

        events.clear();
    }

    @Test
    public void accessTokenCodeExpired() {
        getTestingClient().testing().setTestingInfinispanTimeService();
        RealmManager.realm(adminClient.realm("test")).accessCodeLifeSpan(1);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        try {
            setTimeOffset(2);

            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
            Assert.assertEquals(400, response.getStatusCode());
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
            resetTimeOffset();
        }

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, codeId);
        expectedEvent.error("invalid_code")
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_TYPE)
                .user((String) null);
        expectedEvent.assertEvent();

        events.clear();

        RealmManager.realm(adminClient.realm("test")).accessCodeLifeSpan(60);
    }

    @Test
    public void accessTokenCodeUsed() throws IOException {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(200, response.getStatusCode());
        String accessToken = response.getAccessToken();

        Client jaxrsClient = AdminClientUtil.createResteasyClient();
        try {
            // Check that userInfo can be invoked
            Response userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(jaxrsClient, accessToken);
            UserInfoClientUtil.testSuccessfulUserInfoResponse(userInfoResponse, "test-user@localhost", "test-user@localhost");

            // Check that tokenIntrospection can be invoked
            String introspectionResponse = oauth.introspectAccessTokenWithClientCredential("test-app", "password", accessToken);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(introspectionResponse);
            Assert.assertEquals(true, jsonNode.get("active").asBoolean());
            Assert.assertEquals("test-user@localhost", jsonNode.get("email").asText());

            events.clear();

            // Repeating attempt to exchange code should be refused and invalidate previous clientSession
            response = oauth.doAccessTokenRequest(code, "password");
            Assert.assertEquals(400, response.getStatusCode());

            AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, codeId);
            expectedEvent.error("invalid_code")
                    .removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.REFRESH_TOKEN_ID)
                    .removeDetail(Details.REFRESH_TOKEN_TYPE)
                    .user((String) null);
            expectedEvent.assertEvent();

            // Check that userInfo can't be invoked with invalidated accessToken
            userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(jaxrsClient, accessToken);
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), userInfoResponse.getStatus());
            userInfoResponse.close();

            // Check that tokenIntrospection can't be invoked with invalidated accessToken
            introspectionResponse = oauth.introspectAccessTokenWithClientCredential("test-app", "password", accessToken);
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(introspectionResponse);
            Assert.assertEquals(false, jsonNode.get("active").asBoolean());
            Assert.assertNull(jsonNode.get("email"));

            events.clear();

            RealmManager.realm(adminClient.realm("test")).accessCodeLifeSpan(60);
        } finally {
            jaxrsClient.close();
        }
    }

    @Test
    public void accessTokenCodeRoleMissing() {
        RealmResource realmResource = adminClient.realm("test");
        RoleRepresentation role = RoleBuilder.create().name("tmp-role").build();
        realmResource.roles().create(role);
        UserResource user = findUserByUsernameId(realmResource, "test-user@localhost");
        UserManager.realm(realmResource).user(user).assignRoles(role.getName());

        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        realmResource.roles().deleteRole("tmp-role");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        assertTrue(token.getRealmAccess().isUserInRole("user"));

        events.clear();
    }

    @Test
    public void accessTokenCodeHasRequiredAction() {

        UserResource user = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        UserManager.realm(adminClient.realm("test")).user(user).addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE.toString());

        oauth.doLogin("test-user@localhost", "password");

        String actionURI = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());
        String code = ActionURIUtils.parseQueryParamsFromActionURI(actionURI).get("code");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(400, response.getStatusCode());

        EventRepresentation event = events.poll();
        assertNull(event.getDetails().get(Details.CODE_ID));

        UserManager.realm(adminClient.realm("test")).user(user).removeRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE.toString());
    }

    @Test
    public void testGrantAccessToken() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);

        {   // test checkSsl
            {
                RealmResource realmsResource = adminClient.realm("test");
                RealmRepresentation realmRepresentation = realmsResource.toRepresentation();
                realmRepresentation.setSslRequired(SslRequired.ALL.toString());
                realmsResource.update(realmRepresentation);
            }

            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(AUTH_SERVER_SSL_REQUIRED ? 200 : 403, response.getStatus());
            response.close();

            {
                RealmResource realmsResource = realmsResouce().realm("test");
                RealmRepresentation realmRepresentation = realmsResource.toRepresentation();
                realmRepresentation.setSslRequired(SslRequired.EXTERNAL.toString());
                realmsResource.update(realmRepresentation);
            }

        }

        {   // test null username
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("password", "password");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }

        {   // test no password
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }

        {   // test invalid password
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            form.param("password", "invalid");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }
        {   // test no password
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }

        {   //test bearer-only

            {
                ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
                ClientRepresentation clientRepresentation = clientResource.toRepresentation();
                clientRepresentation.setBearerOnly(true);
                clientResource.update(clientRepresentation);
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            // 401 because the client is now a bearer without a secret
            assertEquals(401, response.getStatus());
            response.close();

            {
                ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
                ClientRepresentation clientRepresentation = clientResource.toRepresentation();
                clientRepresentation.setBearerOnly(false);
                // reset to the old secret
                clientRepresentation.setSecret("password");
                clientResource.update(clientRepresentation);
            }

        }

        {   // test realm disabled
            {
                RealmResource realmsResource = realmsResouce().realm("test");
                RealmRepresentation realmRepresentation = realmsResource.toRepresentation();
                realmRepresentation.setEnabled(false);
                realmsResource.update(realmRepresentation);
            }

            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(403, response.getStatus());
            response.close();

            {
                RealmResource realmsResource = realmsResouce().realm("test");
                RealmRepresentation realmRepresentation = realmsResource.toRepresentation();
                realmRepresentation.setEnabled(true);
                realmsResource.update(realmRepresentation);
            }

        }

        {   // test application disabled

            {
                ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
                ClientRepresentation clientRepresentation = clientResource.toRepresentation();
                clientRepresentation.setEnabled(false);
                clientResource.update(clientRepresentation);
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(400, response.getStatus());
            response.close();

            {
                ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
                ClientRepresentation clientRepresentation = clientResource.toRepresentation();
                clientRepresentation.setEnabled(true);
                clientResource.update(clientRepresentation);

            }

        }

        {   // test user action required

            {
                UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
                userResource.update(userRepresentation);
            }

            // good password is 400 => Account is not fully set up
            try (Response response = executeGrantAccessTokenRequest(grantTarget)) {
                assertEquals(400, response.getStatus());
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.readEntity(String.class));
                assertEquals("invalid_grant", jsonNode.get("error").asText());
                assertEquals("Account is not fully set up", jsonNode.get("error_description").asText());
            }

            // wrong password is 401 => Invalid user credentials
            try (Response response = executeGrantAccessTokenRequestWrongPassword(grantTarget)) {
                assertEquals(401, response.getStatus());
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.readEntity(String.class));
                assertEquals("invalid_grant", jsonNode.get("error").asText());
                assertEquals("Invalid user credentials", jsonNode.get("error_description").asText());
            }

            {
                UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.getRequiredActions().remove(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
                userResource.update(userRepresentation);
            }

        }

        {   // test user disabled
            {
                UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.setEnabled(false);
                userResource.update(userRepresentation);
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(400, response.getStatus());
            response.close();

            {
                UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.setEnabled(true);
                userResource.update(userRepresentation);
            }

        }

        {
            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            response.close();
        }

        client.close();
        events.clear();
    }

    @Test
    public void testKeycloak2221() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);

        ClientResource clientResource;

        {

            clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
            clientResource.getProtocolMappers().createMapper(createRoleNameMapper("rename-role", "user", "realm-user"));
            clientResource.getProtocolMappers().createMapper(createRoleNameMapper("rename-role2", "admin", "the-admin"));

        }

        {
            Response response = executeGrantRequest(grantTarget, "no-permissions", "password");
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            AccessToken accessToken = getAccessToken(tokenResponse);
            assertEquals(accessToken.getRealmAccess().getRoles().size(), 1);
            assertTrue(accessToken.getRealmAccess().getRoles().contains("realm-user"));

            response.close();
        }

        // undo mappers
        {
            ClientResource app = findClientByClientId(adminClient.realm("test"), "test-app");
            ClientRepresentation clientRepresentation = app.toRepresentation();
            for (ProtocolMapperRepresentation protocolRep : clientRepresentation.getProtocolMappers()) {
                if (protocolRep.getName().startsWith("rename-role")) {
                    clientResource.getProtocolMappers().delete(protocolRep.getId());
                }
            }
        }

        events.clear();

    }

    @Test
    public void testClientScope() throws Exception {
        RealmResource realm = adminClient.realm("test");
        RoleRepresentation realmRole = new RoleRepresentation();
        realmRole.setName("realm-test-role");
        realm.roles().create(realmRole);
        realmRole = realm.roles().get("realm-test-role").toRepresentation();
        RoleRepresentation realmRole2 = new RoleRepresentation();
        realmRole2.setName("realm-test-role2");
        realm.roles().create(realmRole2);
        realmRole2 = realm.roles().get("realm-test-role2").toRepresentation();


        List<UserRepresentation> users = realm.users().search("test-user@localhost", -1, -1);
        assertEquals(1, users.size());
        UserRepresentation user = users.get(0);

        List<RoleRepresentation> addRoles = new LinkedList<>();
        addRoles.add(realmRole);
        addRoles.add(realmRole2);
        realm.users().get(user.getId()).roles().realmLevel().add(addRoles);

        ClientScopeRepresentation rep = new ClientScopeRepresentation();
        rep.setName("scope");
        rep.setProtocol("openid-connect");
        Response response = realm.clientScopes().create(rep);
        assertEquals(201, response.getStatus());
        URI scopeUri = response.getLocation();
        String clientScopeId = ApiUtil.getCreatedId(response);
        response.close();
        ClientScopeResource clientScopeResource = adminClient.proxy(ClientScopeResource.class, scopeUri);
        ProtocolMapperModel hard = HardcodedClaim.create("hard", "hard", "coded", "String", true, true);
        ProtocolMapperRepresentation mapper = ModelToRepresentation.toRepresentation(hard);
        response = clientScopeResource.getProtocolMappers().createMapper(mapper);
        assertEquals(201, response.getStatus());
        response.close();

        ClientRepresentation clientRep = ApiUtil.findClientByClientId(realm, "test-app").toRepresentation();
        realm.clients().get(clientRep.getId()).addDefaultClientScope(clientScopeId);
        clientRep.setFullScopeAllowed(false);
        realm.clients().get(clientRep.getId()).update(clientRep);

        {
            Client client = AdminClientUtil.createResteasyClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            IDToken idToken = getIdToken(tokenResponse);
            assertEquals("coded", idToken.getOtherClaims().get("hard"));

            AccessToken accessToken = getAccessToken(tokenResponse);
            assertEquals("coded", accessToken.getOtherClaims().get("hard"));

            // check zero scope for client scope
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // test that scope is added
        List<RoleRepresentation> addRole1 = new LinkedList<>();
        addRole1.add(realmRole);
        clientScopeResource.getScopeMappings().realmLevel().add(addRole1);

        {
            Client client = AdminClientUtil.createResteasyClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            AccessToken accessToken = getAccessToken(tokenResponse);
            // check single role in scope for client scope
            assertNotNull(accessToken.getRealmAccess());
            assertTrue(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // test combined scopes
        List<RoleRepresentation> addRole2 = new LinkedList<>();
        addRole2.add(realmRole2);
        realm.clients().get(clientRep.getId()).getScopeMappings().realmLevel().add(addRole2);

        {
            Client client = AdminClientUtil.createResteasyClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);

            AccessToken accessToken = getAccessToken(tokenResponse);

            // check zero scope for client scope
            assertNotNull(accessToken.getRealmAccess());
            assertTrue(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            assertTrue(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // remove scopes and retest
        clientScopeResource.getScopeMappings().realmLevel().remove(addRole1);
        realm.clients().get(clientRep.getId()).getScopeMappings().realmLevel().remove(addRole2);

        {
            Client client = AdminClientUtil.createResteasyClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);

            AccessToken accessToken = getAccessToken(tokenResponse);
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // test don't use client scope scope. Add roles back to the clientScope, but they won't be available
        realm.clients().get(clientRep.getId()).removeDefaultClientScope(clientScopeId);
        clientScopeResource.getScopeMappings().realmLevel().add(addRole1);
        clientScopeResource.getScopeMappings().realmLevel().add(addRole2);

        {
            Client client = AdminClientUtil.createResteasyClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);

            AccessToken accessToken = getAccessToken(tokenResponse);
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));
            assertNull(accessToken.getOtherClaims().get("hard"));

            IDToken idToken = getIdToken(tokenResponse);
            assertNull(idToken.getOtherClaims().get("hard"));

            response.close();
            client.close();
        }

        // undo mappers
        realm.users().get(user.getId()).roles().realmLevel().remove(addRoles);
        realm.roles().get(realmRole.getName()).remove();
        realm.roles().get(realmRole2.getName()).remove();
        clientScopeResource.remove();

        {
            Client client = AdminClientUtil.createResteasyClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            IDToken idToken = getIdToken(tokenResponse);
            assertNull(idToken.getOtherClaims().get("hard"));

            AccessToken accessToken = getAccessToken(tokenResponse);
            assertNull(accessToken.getOtherClaims().get("hard"));


            response.close();
            client.close();
        }
        events.clear();

    }

    // KEYCLOAK-1595 Assert that public client is able to retrieve token even if header "Authorization: Negotiate something" was used (parameter client_id has preference in this case)
    @Test
    public void testAuthorizationNegotiateHeaderIgnored() throws Exception {

        adminClient.realm("test").clients().create(ClientBuilder.create()
                .clientId("sample-public-client")
                .authenticatorType("client-secret")
                .redirectUris(oauth.getRedirectUri() + "/*")
                .publicClient()
                .build());

        oauth.clientId("sample-public-client");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().client("sample-public-client").assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getAccessTokenUrl());

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
            post.setHeader("Authorization", "Negotiate something-which-will-be-ignored");

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            post.setEntity(formEntity);

            OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(client.execute(post));
            Assert.assertEquals(200, response.getStatusCode());
            AccessToken token = oauth.verifyToken(response.getAccessToken());
            events.expectCodeToToken(codeId, sessionId).client("sample-public-client").assertEvent();
        }
    }

    // KEYCLOAK-4215
    @Test
    public void expiration() throws Exception {
        int sessionMax = (int) TimeUnit.MINUTES.toSeconds(30);
        int sessionIdle = (int) TimeUnit.MINUTES.toSeconds(30);
        int tokenLifespan = (int) TimeUnit.MINUTES.toSeconds(5);

        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();
        Integer originalSessionMax = rep.getSsoSessionMaxLifespan();
        rep.setSsoSessionMaxLifespan(sessionMax);
        realm.update(rep);

        try {
            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
            assertEquals(200, response.getStatusCode());

            // Assert refresh expiration equals session idle
            assertExpiration(response.getRefreshExpiresIn(), sessionIdle);

            // Assert token expiration equals token lifespan
            assertExpiration(response.getExpiresIn(), tokenLifespan);

            setTimeOffset(sessionMax - 60);

            response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
            assertEquals(200, response.getStatusCode());

            // Assert expiration equals session expiration
            assertExpiration(response.getRefreshExpiresIn(), 60);
            assertExpiration(response.getExpiresIn(), 60);
        } finally {
            rep.setSsoSessionMaxLifespan(originalSessionMax);
            realm.update(rep);
        }
    }

    @Test
    public void accessTokenResponseHeader() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());

        Map<String, String> headers = response.getHeaders();
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("no-store", headers.get("Cache-Control"));
        assertEquals("no-cache", headers.get("Pragma"));
    }

    private IDToken getIdToken(org.keycloak.representations.AccessTokenResponse tokenResponse) throws JWSInputException {
        JWSInput input = new JWSInput(tokenResponse.getIdToken());
        return input.readJsonContent(IDToken.class);
    }

    private AccessToken getAccessToken(org.keycloak.representations.AccessTokenResponse tokenResponse) throws JWSInputException {
        JWSInput input = new JWSInput(tokenResponse.getToken());
        return input.readJsonContent(AccessToken.class);
    }

    protected Response executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String username = "test-user@localhost";
        String password = "password";
        return executeGrantRequest(grantTarget, username, password);
    }

    protected Response executeGrantAccessTokenRequestWrongPassword(WebTarget grantTarget) {
        return executeGrantRequest(grantTarget, "test-user@localhost", "bad-password");
    }

    protected Response executeGrantRequest(WebTarget grantTarget, String username, String password) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", username)
                .param("password", password)
                .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID);
        return grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }

    @Test
    public void clientAccessTokenLifespanOverride() {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = client.toRepresentation();

        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();

        int sessionMax = rep.getSsoSessionMaxLifespan();
        int accessTokenLifespan = rep.getAccessTokenLifespan();

        // Make sure realm lifespan is not same as client override
        assertNotEquals(accessTokenLifespan, 500);

        try {
            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_LIFESPAN, "500");
            client.update(clientRep);

            oauth.doLogin("test-user@localhost", "password");

            // Check access token expires in 500 seconds as specified on client

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
            assertEquals(200, response.getStatusCode());

            assertExpiration(response.getExpiresIn(), 500);

            // Check access token expires when session expires

            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_LIFESPAN, "-1");
            client.update(clientRep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken, "password");
            assertEquals(200, response.getStatusCode());

            assertExpiration(response.getExpiresIn(), sessionMax);
        } finally {
            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_LIFESPAN, null);
            client.update(clientRep);
        }
    }

    @Test
    public void testClientSessionMaxLifespan() throws Exception {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();
        int accessTokenLifespan = rep.getAccessTokenLifespan();
        Integer originalClientSessionMaxLifespan = rep.getClientSessionMaxLifespan();

        try {
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getExpiresIn(), accessTokenLifespan);

            rep.setClientSessionMaxLifespan(accessTokenLifespan - 100);
            realm.update(rep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken, "password");
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getExpiresIn(), accessTokenLifespan - 100);

            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN,
                Integer.toString(accessTokenLifespan - 200));
            client.update(clientRepresentation);

            refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken, "password");
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getExpiresIn(), accessTokenLifespan - 200);
        } finally {
            rep.setClientSessionMaxLifespan(originalClientSessionMaxLifespan);
            realm.update(rep);
            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN, null);
            client.update(clientRepresentation);
        }
    }

    @Test
    public void testClientOfflineSessionMaxLifespan() throws Exception {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = client.toRepresentation();

        RealmResource realm = adminClient.realm("test");
        RealmRepresentation rep = realm.toRepresentation();
        int accessTokenLifespan = rep.getAccessTokenLifespan();
        Boolean originalOfflineSessionMaxLifespanEnabled = rep.getOfflineSessionMaxLifespanEnabled();
        Integer originalClientOfflineSessionMaxLifespan = rep.getClientOfflineSessionMaxLifespan();

        try {
            rep.setOfflineSessionMaxLifespanEnabled(true);
            realm.update(rep);

            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getExpiresIn(), accessTokenLifespan);

            rep.setClientOfflineSessionMaxLifespan(accessTokenLifespan - 100);
            realm.update(rep);

            String refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken, "password");
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getExpiresIn(), accessTokenLifespan - 100);

            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN,
                Integer.toString(accessTokenLifespan - 200));
            client.update(clientRepresentation);

            refreshToken = response.getRefreshToken();
            response = oauth.doRefreshTokenRequest(refreshToken, "password");
            assertEquals(200, response.getStatusCode());
            assertExpiration(response.getExpiresIn(), accessTokenLifespan - 200);
        } finally {
            rep.setOfflineSessionMaxLifespanEnabled(originalOfflineSessionMaxLifespanEnabled);
            rep.setClientOfflineSessionMaxLifespan(originalClientOfflineSessionMaxLifespan);
            realm.update(rep);
            clientRepresentation.getAttributes().put(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, null);
            client.update(clientRepresentation);
        }
    }

    @Test
    public void accessTokenRequestNoRefreshToken() {
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);

        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());

        assertNotNull(response.getAccessToken());
        assertNull(response.getRefreshToken());

        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "true");
        client.update(clientRepresentation);
    }

    @Test
    public void accessTokenRequest_ClientPS384_RealmRS256() throws Exception {
        conductAccessTokenRequest(Algorithm.HS256, Algorithm.PS384, Algorithm.RS256);
    }

    @Test
    public void accessTokenRequest_ClientPS256_RealmPS256() throws Exception {
        conductAccessTokenRequest(Algorithm.HS256, Algorithm.PS256, Algorithm.PS256);
    }

    @Test
    public void accessTokenRequest_ClientPS512_RealmPS256() throws Exception {
        conductAccessTokenRequest(Algorithm.HS256, Algorithm.PS512, Algorithm.PS256);
    }

    @Test
    public void accessTokenRequest_ClientRS384_RealmRS256() throws Exception {
        conductAccessTokenRequest(Algorithm.HS256, Algorithm.RS384, Algorithm.RS256);
    }

    @Test
    public void accessTokenRequest_ClientRS512_RealmRS512() throws Exception {
        conductAccessTokenRequest(Algorithm.HS256, Algorithm.RS512, Algorithm.RS512);
    }

    @Test
    public void accessTokenRequest_ClientES256_RealmPS256() throws Exception {
        conductAccessTokenRequest(Algorithm.HS256, Algorithm.ES256, Algorithm.PS256);
    }

    @Test
    public void accessTokenRequest_ClientES384_RealmES384() throws Exception {
        conductAccessTokenRequest(Algorithm.HS256, Algorithm.ES384, Algorithm.ES384);
    }

    @Test
    public void accessTokenRequest_ClientES512_RealmRS256() throws Exception {
        conductAccessTokenRequest(Algorithm.HS256, Algorithm.ES512, Algorithm.RS256);
    }

    @Test
    public void validateECDSASignatures() {
        validateTokenECDSASignature(Algorithm.ES256);
        validateTokenECDSASignature(Algorithm.ES384);
        validateTokenECDSASignature(Algorithm.ES512);
    }

    private void validateTokenECDSASignature(String expectedAlg) {
        assertThat(ECDSASignatureProvider.ECDSA.values(), hasItemInArray(ECDSASignatureProvider.ECDSA.valueOf(expectedAlg)));

        try {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, expectedAlg);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), expectedAlg);
            validateTokenSignatureLength(ECDSASignatureProvider.ECDSA.valueOf(expectedAlg).getSignatureLength());
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), Algorithm.RS256);
        }
    }

    private void validateTokenSignatureLength(int expectedLength) {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        String token = response.getAccessToken();
        oauth.verifyToken(token);

        String encodedSignature = token.split("\\.",3)[2];
        byte[] signature = Base64Url.decode(encodedSignature);
        Assert.assertEquals(expectedLength, signature.length);
        oauth.idTokenHint(response.getIdToken()).openLogout();
    }

    private void conductAccessTokenRequest(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        try {
            /// Realm Setting is used for ID Token Signature Algorithm
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, expectedIdTokenAlg);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), expectedAccessAlg);
            tokenRequest(expectedRefreshAlg, expectedAccessAlg, expectedIdTokenAlg);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), Algorithm.RS256);
        }
        return;
    }

    private void tokenRequest(String expectedRefreshAlg, String expectedAccessAlg, String expectedIdTokenAlg) throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());

        assertEquals("Bearer", response.getTokenType());

        JWSHeader header = new JWSInput(response.getAccessToken()).getHeader();
        assertEquals(expectedAccessAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        header = new JWSInput(response.getIdToken()).getHeader();
        assertEquals(expectedIdTokenAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        header = new JWSInput(response.getRefreshToken()).getHeader();
        assertEquals(expectedRefreshAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), token.getSubject());
        assertNotEquals("test-user@localhost", token.getSubject());

        assertEquals(sessionId, token.getSessionState());

        EventRepresentation event = events.expectCodeToToken(codeId, sessionId).assertEvent();
        assertEquals(token.getId(), event.getDetails().get(Details.TOKEN_ID));
        assertEquals(oauth.parseRefreshToken(response.getRefreshToken()).getId(), event.getDetails().get(Details.REFRESH_TOKEN_ID));
        assertEquals(sessionId, token.getSessionState());
    }

    // KEYCLOAK-16009
    @Test
    public void tokenRequestParamsMoreThanOnce() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getAccessTokenUrl());

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "foo"));

            String authorization = BasicAuthHelper.createHeader(OAuth2Constants.CLIENT_ID, "password");
            post.setHeader("Authorization", authorization);

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            post.setEntity(formEntity);

            OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(client.execute(post));
            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_request", response.getError());
            assertEquals("duplicated parameter", response.getErrorDescription());
        }
    }

}
