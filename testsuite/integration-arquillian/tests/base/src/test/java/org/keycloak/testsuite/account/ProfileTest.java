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

package org.keycloak.testsuite.account;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.openqa.selenium.JavascriptExecutor;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.UserBuilder;
import twitter4j.JSONArray;
import twitter4j.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ProfileTest extends TestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
        user.setFirstName("First");
        user.setLastName("Last");
        Map<String, Object> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
            user.setAttributes(attributes);
        }
        attributes.put("key1", "value1");
        attributes.put("key2", "value2");

        UserRepresentation user2 = UserBuilder.create()
                                              .enabled(true)
                                              .username("test-user-no-access@localhost")
                                              .password("password")
                                              .build();
        RealmBuilder.edit(testRealm)
                    .user(user2);

        ClientBuilder.edit(RealmRepUtil.findClientByClientId(testRealm, "test-app"))
                     .addWebOrigin("http://localtest.me:8180");
    }

    private RoleRepresentation findViewProfileRole(ClientResource accountApp) {
        RoleMappingResource scopeMappings = accountApp.getScopeMappings();
        RoleScopeResource clientLevelMappings = scopeMappings.clientLevel(accountApp.toRepresentation().getId());
        List<RoleRepresentation> accountRoleList = clientLevelMappings.listEffective();

        for (RoleRepresentation role : accountRoleList) {
            if (role.getName().equals(AccountRoles.VIEW_PROFILE)) return role;
        }

        return null;
    }

    @Before
    public void addScopeMappings() {
        String accountClientId = org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        ClientResource accountApp = ApiUtil.findClientByClientId(testRealm(), accountClientId);
        RoleRepresentation role = findViewProfileRole(accountApp);

        String accountAppId = accountApp.toRepresentation().getId();
        ClientResource app = ApiUtil.findClientByClientId(testRealm(), "test-app");
        app.getScopeMappings().clientLevel(accountAppId).add(Collections.singletonList(role));

        ClientResource thirdParty = ApiUtil.findClientByClientId(testRealm(), "third-party");
        thirdParty.getScopeMappings().clientLevel(accountAppId).add(Collections.singletonList(role));
    }

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Page
    protected AccountApplicationsPage accountApplicationsPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Test
    public void getProfile() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        HttpResponse response = doGetProfile(token, null);
        assertEquals(200, response.getStatusLine().getStatusCode());
        JSONObject profile = new JSONObject(IOUtils.toString(response.getEntity().getContent()));

        assertEquals("test-user@localhost", profile.getString("username"));
        assertEquals("test-user@localhost", profile.getString("email"));
        assertEquals("First", profile.getString("firstName"));
        assertEquals("Last", profile.getString("lastName"));

        JSONObject attributes = profile.getJSONObject("attributes");
        JSONArray attrValue = attributes.getJSONArray("key1");
        assertEquals(1, attrValue.length());
        assertEquals("value1", attrValue.get(0));
        attrValue = attributes.getJSONArray("key2");
        assertEquals(1, attrValue.length());
        assertEquals("value2", attrValue.get(0));
    }

    @Test
    public void getProfileCors() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        driver.navigate().to("http://localtest.me:8180/app");

        String[] response = doGetProfileJs(token);
        assertEquals("200", response[0]);
    }

    @Test
    public void getProfileCorsInvalidOrigin() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        driver.navigate().to("http://invalid.localtest.me:8180");

        try {
            doGetProfileJs(token);
            fail("Expected failure");
        } catch (Throwable t) {
        }
    }

    @Test
    public void getProfileCookieAuth() throws Exception {
        profilePage.open();
        loginPage.login("test-user@localhost", "password");

        String[] response = doGetProfileJs(null);
        assertEquals("200", response[0]);

        JSONObject profile = new JSONObject(response[1]);
        assertEquals("test-user@localhost", profile.getString("username"));
    }

    @Test
    public void getProfileNoAuth() throws Exception {
        HttpResponse response = doGetProfile(null, null);
        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    @Test
    public void getProfileNoAccess() throws Exception {
        oauth.doLogin("test-user-no-access@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        HttpResponse response = doGetProfile(token, null);
        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    @Test
    public void getProfileOAuthClient() throws Exception {
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.accept();

        String token = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password").getAccessToken();
        HttpResponse response = doGetProfile(token, null);

        assertEquals(200, response.getStatusLine().getStatusCode());
        JSONObject profile = new JSONObject(IOUtils.toString(response.getEntity().getContent()));

        assertEquals("test-user@localhost", profile.getString("username"));

        accountApplicationsPage.open();
        accountApplicationsPage.revokeGrant("third-party");
    }

    @Test
    public void getProfileOAuthClientNoScope() throws Exception {
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        String token = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password").getAccessToken();
        HttpResponse response = doGetProfile(token, null);

        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    private URI getAccountURI() {
        return RealmsResource.accountUrl(UriBuilder.fromUri(oauth.AUTH_SERVER_ROOT)).build(oauth.getRealm());
    }

    private HttpResponse doGetProfile(String token, String origin) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(UriBuilder.fromUri(getAccountURI()).build());
        if (token != null) {
            get.setHeader(HttpHeaders.AUTHORIZATION, "bearer " + token);
        }
        if (origin != null) {
            get.setHeader("Origin", origin);
        }
        get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        return client.execute(get);
    }

    private String[] doGetProfileJs(String token) {
        StringBuilder sb = new StringBuilder();
        sb.append("var req = new XMLHttpRequest();\n");
        sb.append("req.open('GET', '" + getAccountURI().toString() + "', false);\n");
        if (token != null) {
            sb.append("req.setRequestHeader('Authorization', 'Bearer " + token + "');\n");
        }
        sb.append("req.setRequestHeader('Accept', 'application/json');\n");
        sb.append("req.send(null);\n");
        sb.append("return req.status + '///' + req.responseText;\n");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        String response = (String) js.executeScript(sb.toString());
        return response.split("///");
    }
}
