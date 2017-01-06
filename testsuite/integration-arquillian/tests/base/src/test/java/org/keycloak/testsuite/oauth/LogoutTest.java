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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.*;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LogoutTest extends AbstractKeycloakTest {

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
    public void postLogout() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = tokenResponse.getRefreshToken();

        try (CloseableHttpResponse response = oauth.doLogout(refreshTokenString, "password")) {
            assertThat(response, Matchers.statusCodeIsHC(Status.NO_CONTENT));

            assertNotNull(testingClient.testApp().getAdminLogoutAction());
        }
    }

    @Test
    public void postLogoutExpiredRefreshToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = tokenResponse.getRefreshToken();

        adminClient.realm("test").update(RealmBuilder.create().notBefore(Time.currentTime() + 1).build());

        // Logout should succeed with expired refresh token, see KEYCLOAK-3302
        try (CloseableHttpResponse response = oauth.doLogout(refreshTokenString, "password")) {
            assertThat(response, Matchers.statusCodeIsHC(Status.NO_CONTENT));

            assertNotNull(testingClient.testApp().getAdminLogoutAction());
        }
    }

    @Test
    public void postLogoutWithValidIdToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idTokenString = tokenResponse.getIdToken();

        String logoutUrl = oauth.getLogoutUrl()
          .idTokenHint(idTokenString)
          .postLogoutRedirectUri(AppPage.baseUrl)
          .build();
        
        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
          CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(AppPage.baseUrl));
        }
    }

    @Test
    public void postLogoutWithExpiredIdToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idTokenString = tokenResponse.getIdToken();

        // Logout should succeed with expired ID token, see KEYCLOAK-3399
        setTimeOffset(60 * 60 * 24);

        String logoutUrl = oauth.getLogoutUrl()
          .idTokenHint(idTokenString)
          .postLogoutRedirectUri(AppPage.baseUrl)
          .build();

        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
          CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(AppPage.baseUrl));
        }
    }

    @Test
    public void postLogoutWithValidIdTokenWhenLoggedOutByAdmin() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String idTokenString = tokenResponse.getIdToken();

        adminClient.realm("test").logoutAll();

        // Logout should succeed with user already logged out, see KEYCLOAK-3399
        String logoutUrl = oauth.getLogoutUrl()
          .idTokenHint(idTokenString)
          .postLogoutRedirectUri(AppPage.baseUrl)
          .build();

        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
          CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(AppPage.baseUrl));
        }
    }

}
