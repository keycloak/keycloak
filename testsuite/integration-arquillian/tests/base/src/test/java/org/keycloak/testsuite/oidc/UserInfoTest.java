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

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserInfoClientUtil;
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
    public void testSuccess_getMethod_header() throws Exception {
        Client client = ClientBuilder.newClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            testSuccessfulUserInfoResponse(response);

        } finally {
            client.close();
        }
    }

    @Test
    public void testSuccess_postMethod_header() throws Exception {
        Client client = ClientBuilder.newClient();

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
        Client client = ClientBuilder.newClient();

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

    @Test
    public void testSuccess_postMethod_header_textEntity() throws Exception {
        Client client = ClientBuilder.newClient();

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
    public void testSessionExpired() throws Exception {
        Client client = ClientBuilder.newClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            testingClient.testing().removeUserSessions("test");

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.USER_SESSION_NOT_FOUND)
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
    public void testUnsuccessfulUserInfoRequest() throws Exception {
        Client client = ClientBuilder.newClient();

        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, "bad");

            response.close();

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

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

    private AccessTokenResponse executeGrantAccessTokenRequest(Client client) {
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);

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

        events.clear();

        return accessTokenResponse;
    }

    private void testSuccessfulUserInfoResponse(Response response) {
        events.expect(EventType.USER_INFO_REQUEST)
                .session(Matchers.notNullValue(String.class))
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                .detail(Details.USERNAME, "test-user@localhost")
                .assertEvent();
        UserInfoClientUtil.testSuccessfulUserInfoResponse(response, "test-user@localhost", "test-user@localhost");
    }
}
