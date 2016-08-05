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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.openqa.selenium.By;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.OAuthClient.APP_ROOT;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthRedirectUriTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected ErrorPage errorPage;
    @Page
    protected LoginPage loginPage;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        RealmBuilder realm = RealmBuilder.edit(realmRepresentation).testEventListener();

        ClientBuilder installedApp = ClientBuilder.create().id("test-installed").name("test-installed")
                .redirectUris(Constants.INSTALLED_APP_URN, Constants.INSTALLED_APP_URL)
                .secret("password");
        realm.client(installedApp);

        ClientBuilder installedApp2 = ClientBuilder.create().id("test-installed2").name("test-installed2")
                .redirectUris(Constants.INSTALLED_APP_URL + "/myapp")
                .secret("password");
        realm.client(installedApp2);

        ClientBuilder installedApp3 = ClientBuilder.create().id("test-wildcard").name("test-wildcard")
                .redirectUris("http://example.com/foo/*", "http://with-dash.example.com/foo/*", "http://localhost:8180/foo/*")
                .secret("password");
        realm.client(installedApp3);

        ClientBuilder installedApp4 = ClientBuilder.create().id("test-dash").name("test-dash")
                .redirectUris("http://with-dash.example.com", "http://with-dash.example.com/foo")
                .secret("password");
        realm.client(installedApp4);

        ClientBuilder installedApp5 = ClientBuilder.create().id("test-root-url").name("test-root-url")
                .rootUrl("http://with-dash.example.com")
                .redirectUris("/foo")
                .secret("password");
        realm.client(installedApp5);

        ClientBuilder installedApp6 = ClientBuilder.create().id("test-relative-url").name("test-relative-url")
                .rootUrl("")
                .redirectUris("/foo")
                .secret("password");
        realm.client(installedApp6);

        testRealms.add(realm.build());
    }

    @Test
    public void testNoParam() throws IOException {
        oauth.redirectUri(null);
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
    }

    @Test
    public void testNoParamMultipleValidUris() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris("http://localhost:8180/app2");
        try {
            oauth.redirectUri(null);
            oauth.openLoginForm();
            Assert.assertTrue(errorPage.isCurrent());
            Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        } finally {
            ClientManager.realm(adminClient.realm("test")).clientId("test-app").removeRedirectUris("http://localhost:8180/app2");
        }
    }

    @Test
    public void testNoParamNoValidUris() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app")
                .removeRedirectUris("http://localhost:8180/auth/realms/master/app/auth/*");
        try {
            oauth.redirectUri(null);
            oauth.openLoginForm();

            Assert.assertTrue(errorPage.isCurrent());
            assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        } finally {
            ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris("http://localhost:8180/auth/realms/master/app/auth/*");
        }
    }

    @Test
    public void testNoValidUris() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").removeRedirectUris("http://localhost:8180/auth/realms/master/app/auth/*");

        try {
            oauth.redirectUri(null);
            oauth.openLoginForm();

            Assert.assertTrue(errorPage.isCurrent());
            assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        } finally {
            ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris("http://localhost:8180/auth/realms/master/app/auth/*");
        }
    }

    @Test
    public void testValid() throws IOException {
        oauth.redirectUri(APP_ROOT + "/auth");
        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        URL url = new URL(driver.getCurrentUrl());
        Assert.assertTrue(url.toString().startsWith(APP_ROOT));
        Assert.assertTrue(url.getQuery().contains("code="));
        Assert.assertTrue(url.getQuery().contains("state="));
    }

    @Test
    public void testInvalid() throws IOException {
        oauth.redirectUri("http://localhost:8180/app2");
        oauth.openLoginForm();

        Assert.assertTrue(errorPage.isCurrent());
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
    }

    @Test
    public void testWithParams() throws IOException {
        oauth.redirectUri(APP_ROOT + "/auth?key=value");
        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        URL url = new URL(driver.getCurrentUrl());
        Assert.assertTrue(url.toString().startsWith(APP_ROOT));
        Assert.assertTrue(url.getQuery().contains("key=value"));
        Assert.assertTrue(url.getQuery().contains("state="));
        Assert.assertTrue(url.getQuery().contains("code="));
    }

    @Test
    public void testWildcard() throws IOException {
        oauth.clientId("test-wildcard");
        checkRedirectUri("http://example.com", false);
        checkRedirectUri("http://localhost:8080", false, true);
        checkRedirectUri("http://example.com/foo", true);
        checkRedirectUri("http://example.com/foo/bar", true);
        checkRedirectUri("http://localhost:8180/foo", true, true);
        checkRedirectUri("http://localhost:8180/foo/bar", true, true);
        checkRedirectUri("http://example.com/foobar", false);
        checkRedirectUri("http://localhost:8180/foobar", false, true);
    }

    @Test
    public void testDash() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("http://with-dash.example.com/foo", true);
    }

    @Test
    public void testDifferentCaseInHostname() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("http://with-dash.example.com", true);
        checkRedirectUri("http://wiTh-dAsh.example.com", true);
        checkRedirectUri("http://with-dash.example.com/foo", true);
        checkRedirectUri("http://wiTh-dAsh.example.com/foo", true);
        checkRedirectUri("http://with-dash.eXampLe.com/foo", true);
        checkRedirectUri("http://wiTh-dAsh.eXampLe.com/foo", true);
        checkRedirectUri("http://wiTh-dAsh.eXampLe.com/Foo", false);
        checkRedirectUri("http://wiTh-dAsh.eXampLe.com/foO", false);
    }

    @Test
    public void testDifferentCaseInScheme() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("HTTP://with-dash.example.com", true);
        checkRedirectUri("Http://wiTh-dAsh.example.com", true);
    }

    @Test
    public void testRelativeWithRoot() throws IOException {
        oauth.clientId("test-root-url");

        checkRedirectUri("http://with-dash.example.com/foo", true);
        checkRedirectUri("http://localhost:8180/foo", false);
    }

    @Test
    public void testRelative() throws IOException {
        oauth.clientId("test-relative-url");

        checkRedirectUri("http://with-dash.example.com/foo", false);
        checkRedirectUri("http://localhost:8180/foo", true);
    }

    @Test
    public void testLocalhost() throws IOException {
        oauth.clientId("test-installed");

        checkRedirectUri("urn:ietf:wg:oauth:2.0:oob", true, true);
        checkRedirectUri("http://localhost", true);

        checkRedirectUri("http://localhost:8280", true, true);

        checkRedirectUri("http://localhosts", false);
        checkRedirectUri("http://localhost/myapp", false);
        checkRedirectUri("http://localhost:8180/myapp", false, true);
        oauth.clientId("test-installed2");

        checkRedirectUri("http://localhost/myapp", true);
        checkRedirectUri("http://localhost:8280/myapp", true, true);

        checkRedirectUri("http://localhosts/myapp", false);
        checkRedirectUri("http://localhost", false);
        checkRedirectUri("http://localhost/myapp2", false);
    }

    private void checkRedirectUri(String redirectUri, boolean expectValid) throws IOException {
        checkRedirectUri(redirectUri, expectValid, false);
    }

    private void checkRedirectUri(String redirectUri, boolean expectValid, boolean checkCodeToToken) throws IOException {
        oauth.redirectUri(redirectUri);
        oauth.openLoginForm();

        if (expectValid) {
            Assert.assertTrue(loginPage.isCurrent());
        } else {
            Assert.assertTrue(errorPage.isCurrent());
            Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        }

        if (expectValid) {
            Assert.assertTrue(loginPage.isCurrent());

            if (checkCodeToToken) {
                oauth.doLogin("test-user@localhost", "password");

                /*
                 * Dirty workaround. For some reason the form is not being submitted when you have
                 * redirectUri like http://localhost:8180 or http://localhost:8180/myapp
                 * TODO: Revisit this, because it's a weird behavior
                 */
                if (driver.findElements(By.name("login")).size() != 0) {
                    driver.findElement(By.name("login")).click();
                }

                String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
                Assert.assertNotNull(code);

                OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

                Assert.assertEquals("Expected success, but got error: " + tokenResponse.getError(), 200, tokenResponse.getStatusCode());

                oauth.doLogout(tokenResponse.getRefreshToken(), "password");
            }
        }
    }

}
