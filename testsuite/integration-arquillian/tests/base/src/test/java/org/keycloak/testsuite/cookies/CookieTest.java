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

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.RealmBuilder;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 *
 * @author hmlnarik
 */
public class CookieTest extends AbstractKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        RealmBuilder realm = RealmBuilder.edit(realmRepresentation).testEventListener();
        RealmRepresentation testRealm = realm.build();
        testRealms.add(testRealm);
    }

    @Test
    public void testCookieValue() throws Exception {
        accountPage.setAuthRealm(AuthRealm.TEST);

        final String accountClientId = realmsResouce().realm("test").clients().findByClientId("account").get(0).getId();
        final String clientSecret = realmsResouce().realm("test").clients().get(accountClientId).getSecret().getValue();

        AuthorizationEndpointResponse codeResponse = oauth.clientId("account").redirectUri(accountPage.buildUri().toString()).doLogin("test-user@localhost", "password");
        OAuthClient.AccessTokenResponse accTokenResp = oauth.doAccessTokenRequest(codeResponse.getCode(), clientSecret);
        String accessToken = accTokenResp.getAccessToken();

        accountPage.navigateTo();
        accountPage.assertCurrent();

        try (CloseableHttpClient hc = OAuthClient.newCloseableHttpClient()) {
            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE, accessToken);
            cookie.setDomain("localhost");
            cookie.setPath("/");
            cookieStore.addCookie(cookie);

            HttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

            HttpGet get = new HttpGet(oauth.clientId("account").redirectUri(accountPage.buildUri().toString()).getLoginFormUrl());
            try (CloseableHttpResponse resp = hc.execute(get, localContext)) {
                final String pageContent = EntityUtils.toString(resp.getEntity());
                
                // Ensure that we did not get to the account page ...
                assertThat(pageContent, not(containsString("First name")));
                assertThat(pageContent, not(containsString("Last name")));

                // ... but were redirected to login page
                assertThat(pageContent, containsString("Log In"));
                assertThat(pageContent, containsString("Forgot Password?"));
            }
        }
    }

    @Test
    public void testCookieValueLoggedOut() throws Exception {
        accountPage.setAuthRealm(AuthRealm.TEST);

        final String accountClientId = realmsResouce().realm("test").clients().findByClientId("account").get(0).getId();
        final String clientSecret = realmsResouce().realm("test").clients().get(accountClientId).getSecret().getValue();

        AuthorizationEndpointResponse codeResponse = oauth.clientId("account").redirectUri(accountPage.buildUri().toString()).doLogin("test-user@localhost", "password");
        OAuthClient.AccessTokenResponse accTokenResp = oauth.doAccessTokenRequest(codeResponse.getCode(), clientSecret);
        String accessToken = accTokenResp.getAccessToken();

        accountPage.navigateTo();
        accountPage.assertCurrent();
        accountPage.logOut();

        try (CloseableHttpClient hc = OAuthClient.newCloseableHttpClient()) {
            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie(AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE, accessToken);
            cookie.setDomain("localhost");
            cookie.setPath("/");
            cookieStore.addCookie(cookie);

            HttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

            HttpGet get = new HttpGet(oauth.clientId("account").redirectUri(accountPage.buildUri().toString()).getLoginFormUrl());
            try (CloseableHttpResponse resp = hc.execute(get, localContext)) {
                final String pageContent = EntityUtils.toString(resp.getEntity());

                // Ensure that we did not get to the account page ...
                assertThat(pageContent, not(containsString("First name")));
                assertThat(pageContent, not(containsString("Last name")));

                // ... but were redirected to login page
                assertThat(pageContent, containsString("Log In"));
                assertThat(pageContent, containsString("Forgot Password?"));
            }
        }
    }

}
