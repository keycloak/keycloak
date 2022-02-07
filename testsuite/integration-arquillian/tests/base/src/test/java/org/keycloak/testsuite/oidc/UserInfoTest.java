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

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;
import org.keycloak.testsuite.util.RoleBuilder;

/**
 * @author pedroigor
 */
public class UserInfoTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        RealmBuilder realm = RealmBuilder.edit(realmRepresentation).testEventListener();
        RealmRepresentation testRealm = realm.build();
        testRealms.add(testRealm);

        ClientRepresentation samlApp = KeycloakModelUtils.createClient(testRealm, "saml-client");
        samlApp.setSecret("secret");
        samlApp.setServiceAccountsEnabled(true);
        samlApp.setDirectAccessGrantsEnabled(true);
    }

    @Test
    public void testSuccess_getMethod_header() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            UserInfo userInfo = testSuccessfulUserInfoResponse(response);
            testRolesAreNotInUserInfoResponse(userInfo);

        } finally {
            client.close();
        }
    }

    @Test
    public void testSuccess_postMethod_header() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getToken())
                    .post(Entity.form(new Form()));

            testSuccessfulUserInfoResponse(response);

        } finally {
            client.close();
        }
    }

    @Test
    public void testSuccess_postMethod_body() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Form form = new Form();
            form.param("access_token", accessTokenResponse.getToken());

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .post(Entity.form(form));

            testSuccessfulUserInfoResponse(response);

        } finally {
            client.close();
        }
    }


    // KEYCLOAK-8838
    @Test
    public void testSuccess_dotsInClientId() throws Exception {
        // Create client with dot in the name
        ClientRepresentation clientRep = org.keycloak.testsuite.util.ClientBuilder.create()
                .clientId("my.foo.client")
                .addRedirectUri("http://foo.host")
                .secret("password")
                .directAccessGrants()
                .build();

        RealmResource realm = adminClient.realm("test");

        Response resp = realm.clients().create(clientRep);
        String clientUUID = ApiUtil.getCreatedId(resp);
        resp.close();
        getCleanup().addClientUuid(clientUUID);

        //Create role with dot in the name
        realm.clients().get(clientUUID).roles().create(RoleBuilder.create().name("my.foo.role").build());

        // Assign role to the user
        RoleRepresentation fooRole = realm.clients().get(clientUUID).roles().get("my.foo.role").toRepresentation();
        UserResource userResource = ApiUtil.findUserByUsernameId(realm, "test-user@localhost");
        userResource.roles().clientLevel(clientUUID).add(Collections.singletonList(fooRole));

        // Login to the new client
        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.clientId("my.foo.client")
                .doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        AccessToken accessToken = oauth.verifyToken(accessTokenResponse.getAccessToken());
        Assert.assertNames(accessToken.getResourceAccess("my.foo.client").getRoles(), "my.foo.role");

        events.clear();

        // Send UserInfo request and ensure it is correct
        Client client = AdminClientUtil.createResteasyClient();
        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getAccessToken());

            testSuccessfulUserInfoResponse(response, "my.foo.client");
        } finally {
            client.close();
        }
    }

    @Test
    public void testSuccess_postMethod_header_textEntity() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getToken())
                    .post(Entity.text(""));

            testSuccessfulUserInfoResponse(response);

        } finally {
            client.close();
        }
    }

    @Test
    public void testSuccessSignedResponse() throws Exception {
        // Require signed userInfo request
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(Algorithm.RS256);
        clientResource.update(clientRep);

        // test signed response
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            events.expect(EventType.USER_INFO_REQUEST)
                    .session(Matchers.notNullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.SIGNATURE_REQUIRED, "true")
                    .detail(Details.SIGNATURE_ALGORITHM, Algorithm.RS256.toString())
                    .assertEvent();

            // Check signature and content
            PublicKey publicKey = PemUtils.decodePublicKey(ApiUtil.findActiveSigningKey(adminClient.realm("test")).getPublicKey());

            Assert.assertEquals(200, response.getStatus());
            Assert.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JWT);
            String signedResponse = response.readEntity(String.class);
            response.close();

            JWSInput jwsInput = new JWSInput(signedResponse);
            Assert.assertTrue(RSAProvider.verify(jwsInput, publicKey));

            UserInfo userInfo = JsonSerialization.readValue(jwsInput.getContent(), UserInfo.class);

            Assert.assertNotNull(userInfo);
            Assert.assertNotNull(userInfo.getSubject());
            Assert.assertEquals("test-user@localhost", userInfo.getEmail());
            Assert.assertEquals("test-user@localhost", userInfo.getPreferredUsername());

            Assert.assertTrue(userInfo.hasAudience("test-app"));
            String expectedIssuer = Urls.realmIssuer(new URI(AUTH_SERVER_ROOT), "test");
            Assert.assertEquals(expectedIssuer, userInfo.getIssuer());

        } finally {
            client.close();
        }

        // Revert signed userInfo request
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(null);
        clientResource.update(clientRep);
    }

    @Test
    public void testSuccessSignedResponseES256() throws Exception {
        testSuccessSignedResponse(Algorithm.ES256);
    }

    @Test
    public void testSuccessSignedResponsePS256() throws Exception {
        testSuccessSignedResponse(Algorithm.PS256);
    }
 
    @Test
    public void testSessionExpired() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            String realmName = "test";
            testingClient.testing().removeUserSessions(realmName);

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("realm=\"" + realmName + "\""));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_REQUEST + "\""));

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.USER_SESSION_NOT_FOUND)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .assertEvent();

        } finally {
            client.close();
        }
    }

    @Test
    public void testAccessTokenExpired() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            setTimeOffset(600);

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .client((String) null)
                    .assertEvent();
        } finally {
            client.close();
        }
    }

    @Test
    public void testAccessTokenAfterUserSessionLogoutAndLoginAgain() {
        OAuthClient.AccessTokenResponse accessTokenResponse = loginAndForceNewLoginPage();
        String refreshToken1 = accessTokenResponse.getRefreshToken();

        oauth.doLogout(refreshToken1, "password");
        events.clear();

        setTimeOffset(2);

        WaitUtils.waitForPageToLoad();
        loginPage.login("password");
        events.expectLogin().assertEvent();

        Assert.assertFalse(loginPage.isCurrent());

        events.clear();

        Client client = AdminClientUtil.createResteasyClient();

        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getAccessToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .client("test-app")
                    .assertEvent();
        } finally {
            client.close();
        }
    }

    @Test
    public void testNotBeforeTokens() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            int time = Time.currentTime() + 60;

            RealmResource realm = adminClient.realm("test");
            RealmRepresentation rep = realm.toRepresentation();
            rep.setNotBefore(time);
            realm.update(rep);

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .client((String) null)
                    .assertEvent();

            events.clear();
            rep.setNotBefore(0);
            realm.update(rep);

            // do the same with client's notBefore
            ClientResource clientResource = realm.clients().get(realm.clients().findByClientId("test-app").get(0).getId());
            ClientRepresentation clientRep = clientResource.toRepresentation();
            clientRep.setNotBefore(time);
            clientResource.update(clientRep);

            response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .client((String) null)
                    .assertEvent();

            clientRep.setNotBefore(0);
            clientResource.update(clientRep);
        } finally {
            client.close();
        }
    }

    @Test
    public void testSessionExpiredOfflineAccess() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client, true);

            testingClient.testing().removeUserSessions("test");

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            testSuccessfulUserInfoResponse(response);
            response.close();
        } finally {
            client.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequest() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, "bad");

            response.close();

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .client((String) null)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .assertEvent();

        } finally {
            client.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithEmptyAccessToken() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, "");
            response.close();
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        } finally {
            client.close();
        }
    }

    @Test
    public void testUserInfoRequestWithSamlClient() throws Exception {
        // obtain an access token
        String accessToken = oauth.doGrantAccessTokenRequest("test", "test-user@localhost", "password", null, "saml-client", "secret").getAccessToken();

        // change client's protocol
        ClientRepresentation samlClient = adminClient.realm("test").clients().findByClientId("saml-client").get(0);
        samlClient.setProtocol("saml");
        adminClient.realm("test").clients().get(samlClient.getId()).update(samlClient);

        Client client = AdminClientUtil.createResteasyClient();
        try {
            events.clear();
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessToken);
            response.close();

            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            events.expect(EventType.USER_INFO_REQUEST)
                    .error(Errors.INVALID_CLIENT)
                    .client((String) null)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .assertEvent();
        } finally {
            client.close();
        }
    }

    @Test
    public void testRolesAreAvailable_getMethod_header() throws Exception {

        switchIncludeRolesInUserInfoEndpoint(true);

        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            UserInfo userInfo = testSuccessfulUserInfoResponse(response);
            testRolesInUserInfoResponse(userInfo);
        } finally {
            client.close();
            switchIncludeRolesInUserInfoEndpoint(false);
        }
    }

    private AccessTokenResponse executeGrantAccessTokenRequest(Client client) {
        return executeGrantAccessTokenRequest(client, false);
    }

    private AccessTokenResponse executeGrantAccessTokenRequest(Client client, boolean requestOfflineToken) {
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);

        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "test-user@localhost")
                .param("password", "password");
        if( requestOfflineToken) {
            form.param("scope", "offline_access");
        }

        Response response = grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));

        assertEquals(200, response.getStatus());

        AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);

        response.close();

        events.clear();

        return accessTokenResponse;
    }

    private UserInfo testSuccessfulUserInfoResponse(Response response) {
        return testSuccessfulUserInfoResponse(response, "test-app");
    }

    private UserInfo testSuccessfulUserInfoResponse(Response response, String expectedClientId) {
        events.expect(EventType.USER_INFO_REQUEST)
                .session(Matchers.notNullValue(String.class))
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.SIGNATURE_REQUIRED, "false")
                .client(expectedClientId)
                .assertEvent();
        return UserInfoClientUtil.testSuccessfulUserInfoResponse(response, "test-user@localhost", "test-user@localhost");
    }

    private void testSuccessSignedResponse(Algorithm sigAlg) throws Exception {

        try {
            // Require signed userInfo request
            ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            ClientRepresentation clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(sigAlg);
            clientResource.update(clientRep);

            // test signed response
            Client client = AdminClientUtil.createResteasyClient();

            try {
                AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

                Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

                events.expect(EventType.USER_INFO_REQUEST)
                        .session(Matchers.notNullValue(String.class))
                        .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                        .detail(Details.USERNAME, "test-user@localhost")
                        .detail(Details.SIGNATURE_REQUIRED, "true")
                        .detail(Details.SIGNATURE_ALGORITHM, sigAlg.toString())
                        .assertEvent();

                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JWT);
                String signedResponse = response.readEntity(String.class);
                response.close();

                JWSInput jwsInput = new JWSInput(signedResponse);

                assertEquals(sigAlg.toString(), jwsInput.getHeader().getAlgorithm().name());

                UserInfo userInfo = JsonSerialization.readValue(jwsInput.getContent(), UserInfo.class);

                Assert.assertNotNull(userInfo);
                Assert.assertNotNull(userInfo.getSubject());
                Assert.assertEquals("test-user@localhost", userInfo.getEmail());
                Assert.assertEquals("test-user@localhost", userInfo.getPreferredUsername());

                Assert.assertTrue(userInfo.hasAudience("test-app"));
                String expectedIssuer = Urls.realmIssuer(new URI(AUTH_SERVER_ROOT), "test");
                Assert.assertEquals(expectedIssuer, userInfo.getIssuer());

            } finally {
                client.close();
            }

            // Revert signed userInfo request
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(null);
            clientResource.update(clientRep);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, org.keycloak.crypto.Algorithm.RS256);
        }
    }

    private OAuthClient.AccessTokenResponse loginAndForceNewLoginPage() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.clientSessionState("client-session");

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        setTimeOffset(1);

        String loginFormUri = UriBuilder.fromUri(oauth.getLoginFormUrl())
                .queryParam(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                .build().toString();
        driver.navigate().to(loginFormUri);

        loginPage.assertCurrent();

        return tokenResponse;
    }

    private void switchIncludeRolesInUserInfoEndpoint(boolean includeRoles) {
        ClientScopesResource clientScopesResource = adminClient.realm("test").clientScopes();
        ClientScopeRepresentation rolesClientScope = clientScopesResource.findAll().stream()
                .filter(clientScope -> "roles".equals(clientScope.getName()))
                .findAny()
                .get();

        ProtocolMappersResource protocolMappersResource =
                clientScopesResource.get(rolesClientScope.getId()).getProtocolMappers();

        ProtocolMapperRepresentation realmRolesMapper = protocolMappersResource.getMappers().stream()
                .filter(mapper -> "realm roles".equals(mapper.getName()))
                .findAny()
                .get();

        realmRolesMapper.getConfig().put(INCLUDE_IN_USERINFO, String.valueOf(includeRoles));

        ProtocolMapperRepresentation clientRolesMapper = protocolMappersResource.getMappers().stream()
                .filter(mapper -> "client roles".equals(mapper.getName()))
                .findAny()
                .get();

        clientRolesMapper.getConfig().put(INCLUDE_IN_USERINFO, String.valueOf(includeRoles));

        protocolMappersResource.update(realmRolesMapper.getId(), realmRolesMapper);
        protocolMappersResource.update(clientRolesMapper.getId(), clientRolesMapper);
    }

    private void testRolesInUserInfoResponse(UserInfo userInfo) {
        Map<String, Collection<String>> realmAccess = (Map<String, Collection<String>>) userInfo.getOtherClaims().get("realm_access");
        Map<String, Map<String, Collection<String>>> resourceAccess = (Map<String, Map<String, Collection<String>>>) userInfo.getOtherClaims().get("resource_access");

        org.hamcrest.MatcherAssert.assertThat(realmAccess.get("roles"), CoreMatchers.hasItems("offline_access", "user"));
        org.hamcrest.MatcherAssert.assertThat(resourceAccess.get("test-app").get("roles"), CoreMatchers.hasItems("customer-user"));
    }

    private void testRolesAreNotInUserInfoResponse(UserInfo userInfo) {
        assertNull(userInfo.getOtherClaims().get("realm_access"));
        assertNull(userInfo.getOtherClaims().get("resource_access"));
    }
}
