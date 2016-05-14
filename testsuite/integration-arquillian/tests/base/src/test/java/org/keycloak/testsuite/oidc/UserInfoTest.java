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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;

/**
 * @author pedroigor
 */
public class UserInfoTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

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
        testRealms.add(realm.build());

    }

    @Test
    public void testSuccessfulUserInfoRequest() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);
        AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(grantTarget);
        Response response = executeUserInfoRequest(accessTokenResponse.getToken());

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        UserInfo userInfo = response.readEntity(UserInfo.class);

        response.close();

        assertNotNull(userInfo);
        assertNotNull(userInfo.getSubject());
        assertEquals("test-user@localhost", userInfo.getEmail());
        assertEquals("test-user@localhost", userInfo.getPreferredUsername());

        client.close();
    }

    @Test
    public void testSessionExpired() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);
        AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(grantTarget);

        testingClient.testing().removeUserSessions("test");

        Response response = executeUserInfoRequest(accessTokenResponse.getToken());

        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());

        response.close();

        client.close();
    }

    @Test
    public void testUnsuccessfulUserInfoRequest() throws Exception {
        Response response = executeUserInfoRequest("bad");

        response.close();

        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    private AccessTokenResponse executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "test-user@localhost")
                .param("password", "password");

        Response response = grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));

        assertEquals(200, response.getStatus());

        AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);

        response.close();

        return accessTokenResponse;
    }

    private Response executeUserInfoRequest(String accessToken) {
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        UriBuilder uriBuilder = OIDCLoginProtocolService.tokenServiceBaseUrl(builder);
        URI userInfoUri = uriBuilder.path(OIDCLoginProtocolService.class, "issueUserInfo").build("test");
        Client client = ClientBuilder.newClient();
        WebTarget userInfoTarget = client.target(userInfoUri);

        return userInfoTarget.request()
                .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                .get();
    }
}
