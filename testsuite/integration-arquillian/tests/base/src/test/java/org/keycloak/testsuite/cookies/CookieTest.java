/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.cookies;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.cookie.CookieType;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.HttpClientUtils;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Cookie;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author hmlnarik
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class CookieTest extends AbstractKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Before
    public void beforeCookieTest() {
        createAppClientInRealm("test");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        RealmBuilder realm = RealmBuilder.edit(realmRepresentation).testEventListener();
        RealmRepresentation testRealm = realm.build();
        testRealms.add(testRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
    }

    @Test
    public void testCookieValue() throws Exception {
        testCookieValue(CookieType.IDENTITY.getName());
    }

    private void testCookieValue(String cookieName) throws Exception {
        final String accountClientId = realmsResouce().realm("test").clients().findByClientId("test-app").get(0).getId();
        final String clientSecret = realmsResouce().realm("test").clients().get(accountClientId).getSecret().getValue();

        AuthorizationEndpointResponse codeResponse = oauth.client("test-app", clientSecret).redirectUri(oauth.APP_AUTH_ROOT).doLogin("test-user@localhost", "password");
        AccessTokenResponse accTokenResp = oauth.doAccessTokenRequest(codeResponse.getCode());
        String accessToken = accTokenResp.getAccessToken();

        appPage.open();
        appPage.assertCurrent();

        try (CloseableHttpClient hc = HttpClientUtils.createDefault()) {
            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie(cookieName, accessToken);
            cookie.setDomain("localhost");
            cookie.setPath("/");
            cookieStore.addCookie(cookie);

            HttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

            HttpGet get = new HttpGet(oauth.clientId("test-app").redirectUri(oauth.APP_AUTH_ROOT).loginForm().build());
            try (CloseableHttpResponse resp = hc.execute(get, localContext)) {
                final String pageContent = EntityUtils.toString(resp.getEntity());

                // Ensure that we did not get to the account page ...
                assertThat(pageContent, not(containsString("First name")));
                assertThat(pageContent, not(containsString("Last name")));

                // ... but were redirected to login page
                assertThat(pageContent, containsString("Sign In"));
                assertThat(pageContent, containsString("Forgot Password?"));
            }
        }
    }

    @Test
    public void testCookieValueLoggedOut() throws Exception {
        final String accountClientId = realmsResouce().realm("test").clients().findByClientId("test-app").get(0).getId();
        final String clientSecret = realmsResouce().realm("test").clients().get(accountClientId).getSecret().getValue();

        AuthorizationEndpointResponse codeResponse = oauth.client("test-app", clientSecret).redirectUri(oauth.APP_AUTH_ROOT).doLogin("test-user@localhost", "password");
        AccessTokenResponse accTokenResp = oauth.doAccessTokenRequest(codeResponse.getCode());
        String accessToken = accTokenResp.getAccessToken();

        appPage.open();
        appPage.assertCurrent();
        AccountHelper.logout(realmsResouce().realm("test"), "test-user@localhost");

        try (CloseableHttpClient hc = HttpClientUtils.createDefault()) {
            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie(CookieType.IDENTITY.getName(), accessToken);
            cookie.setDomain("localhost");
            cookie.setPath("/");
            cookieStore.addCookie(cookie);

            HttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

            HttpGet get = new HttpGet(oauth.clientId("test-app").redirectUri(oauth.APP_AUTH_ROOT).loginForm().build());
            try (CloseableHttpResponse resp = hc.execute(get, localContext)) {
                final String pageContent = EntityUtils.toString(resp.getEntity());

                // Ensure that we did not get to the account page ...
                assertThat(pageContent, not(containsString("First name")));
                assertThat(pageContent, not(containsString("Last name")));

                // ... but were redirected to login page
                assertThat(pageContent, containsString("Sign In"));
                assertThat(pageContent, containsString("Forgot Password?"));
            }
        }
    }

    @Test
    public void testNoDuplicationsWhenExpiringCookies() throws IOException {
        ContainerAssume.assumeAuthServerSSL();

        oauth.doLogin("test-user@localhost", "password");
        appPage.assertCurrent();

        driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/authenticate/");

        Cookie invalidIdentityCookie = driver.manage().getCookieNamed(CookieType.IDENTITY.getName());
        CookieStore cookieStore = new BasicCookieStore();

        BasicClientCookie invalidClientIdentityCookie = new BasicClientCookie(invalidIdentityCookie.getName(), invalidIdentityCookie.getValue());

        invalidClientIdentityCookie.setDomain(invalidIdentityCookie.getDomain());
        invalidClientIdentityCookie.setPath(invalidClientIdentityCookie.getPath());

        cookieStore.addCookie(invalidClientIdentityCookie);

        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).build()) {
            HttpGet get = new HttpGet(
                    suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/" + AuthRealm.TEST + "/protocol/openid-connect/auth?response_type=code&client_id=" + Constants.ACCOUNT_CONSOLE_CLIENT_ID +
                            "&redirect_uri=" + suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/" + AuthRealm.TEST + "/account&scope=openid");

            try (CloseableHttpResponse response = client.execute(get)) {
                Header[] headers = response.getHeaders(HttpHeaders.SET_COOKIE);
                Set<String> cookies = new HashSet<>();

                for (Header header : headers) {
                    assertTrue("Cookie '" + header.getValue() + "' is duplicated", cookies.add(header.getValue()));
                }

                assertFalse(cookies.isEmpty());
            }
        }
    }

}
