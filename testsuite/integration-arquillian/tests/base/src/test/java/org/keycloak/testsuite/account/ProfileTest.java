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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.drone.api.annotation.Default;
import org.jboss.arquillian.graphene.context.GrapheneContext;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestApplicationResource;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.runonserver.SerializationUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import twitter4j.JSONArray;
import twitter4j.JSONObject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ProfileTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
        user.setFirstName("First");
        user.setLastName("Last");
        user.singleAttribute("key1", "value1");
        user.singleAttribute("key2", "value2");

        UserRepresentation user2 = UserBuilder.create()
                                              .enabled(true)
                                              .username("test-user-no-access@localhost")
                                              .password("password")
                                              .build();
        RealmBuilder.edit(testRealm)
                .accessTokenLifespan(1000)
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
        UserRepresentation profile = JsonSerialization.readValue(IOUtils.toString(response.getEntity().getContent()), UserRepresentation.class);

        assertEquals("test-user@localhost", profile.getUsername());
        assertEquals("test-user@localhost", profile.getEmail());
        assertEquals("First", profile.getFirstName());
        assertEquals("Last", profile.getLastName());

        Map<String, List<String>> attributes = profile.getAttributes();
        List<String> attrValue = attributes.get("key1");
        assertEquals(1, attrValue.size());
        assertEquals("value1", attrValue.get(0));
        attrValue = attributes.get("key2");
        assertEquals(1, attrValue.size());
        assertEquals("value2", attrValue.get(0));
    }

    @Test
    public void updateProfile() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("test-user@localhost");
        user.setFirstName("NewFirst");
        user.setLastName("NewLast");
        user.setEmail("NewEmail@localhost");

        HttpResponse response = doUpdateProfile(token, null, JsonSerialization.writeValueAsString(user));
        assertEquals(200, response.getStatusLine().getStatusCode());

        response = doGetProfile(token, null);

        UserRepresentation profile = JsonSerialization.readValue(IOUtils.toString(response.getEntity().getContent()), UserRepresentation.class);

        assertEquals("test-user@localhost", profile.getUsername());
        assertEquals("newemail@localhost", profile.getEmail());
        assertEquals("NewFirst", profile.getFirstName());
        assertEquals("NewLast", profile.getLastName());

        // Revert
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail("test-user@localhost");
        doUpdateProfile(token, null, JsonSerialization.writeValueAsString(user));
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    @Test
    public void getProfileCors() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        driver.navigate().to("http://localtest.me:8180/auth/realms/test/account");

        String[] response = doGetProfileJs("http://localtest.me:8180/auth", token);
        assertEquals("200", response[0]);
    }


    // WARN: If it's failing for phantomJS, make sure to enable CORS by using:
    // -Dphantomjs.cli.args="--ignore-ssl-errors=true --web-security=true"
    @Test
    public void getProfileCorsInvalidOrigin() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        String[] response = null;
        try {
            response = doGetProfileJs("http://invalid.localtest.me:8180/auth", token);
        } catch (WebDriverException ex) {
            // Expected
        }

        // Some webDrivers throw exception (htmlUnit) , some just doesn't return anything.
        if (response != null && response.length > 0 && response[0].equals("200")) {
            fail("Not expected to retrieve response. Make sure CORS are enabled for your browser!");
        }
    }

    @Test
    public void getProfileNoAuth() throws Exception {
        HttpResponse response = doGetProfile(null, null);
        assertEquals(401, response.getStatusLine().getStatusCode());
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

// TODO Test is broken as token is null
//    @Test
//    public void getProfileOAuthClientNoScope() throws Exception {
//        oauth.clientId("third-party");
//        oauth.doLoginGrant("test-user@localhost", "password");
//
//        String token = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password").getAccessToken();
//        HttpResponse response = doGetProfile(token, null);
//
//        assertEquals(403, response.getStatusLine().getStatusCode());
//    }

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

    private HttpResponse doUpdateProfile(String token, String origin, String value) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(UriBuilder.fromUri(getAccountURI()).build());
        if (token != null) {
            post.setHeader(HttpHeaders.AUTHORIZATION, "bearer " + token);
        }
        if (origin != null) {
            post.setHeader("Origin", origin);
        }
        post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        post.setEntity(new StringEntity(value));
        return client.execute(post);
    }

    private String[] doGetProfileJs(String authServerRoot, String token) {
        UriBuilder uriBuilder = UriBuilder.fromUri(authServerRoot)
                .path(TestApplicationResource.class)
                .path(TestApplicationResource.class, "getAccountProfile")
                .queryParam("account-uri", getAccountURI().toString());

        if (token != null) {
            uriBuilder.queryParam("token", token);

            // Remove Keycloak cookies. Some browsers send cookies even in preflight requests
            driver.navigate().to(OAuthClient.AUTH_SERVER_ROOT + "/realms/test/account");
            driver.manage().deleteAllCookies();
        }

        String accountProfileUri = uriBuilder.build().toString();
        log.info("Retrieve profile with URI: " + accountProfileUri);

        driver.navigate().to(accountProfileUri);
        WaitUtils.waitUntilElement(By.id("innerOutput"));
        String response = driver.findElement(By.id("innerOutput")).getText();
        return response.split("///");
    }

    private WebDriver getHtmlUnitDriver() {
        DesiredCapabilities cap = new DesiredCapabilities();
        cap.setPlatform(Platform.ANY);
        cap.setJavascriptEnabled(true);
        cap.setVersion("chrome");
        cap.setBrowserName("htmlunit");
        HtmlUnitDriver driver = new HtmlUnitDriver(cap);
        return driver;
    }
}
