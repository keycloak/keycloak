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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.util.SimpleHttp;
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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
    private HttpServer server;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();

        server = HttpServer.create(new InetSocketAddress(8280), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    @Override
    public void afterAbstractKeycloakTest() {
        super.afterAbstractKeycloakTest();

        server.stop(0);
    }

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String response = "Hello";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
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
                .redirectUris("http://example.com/foo/*", "http://with-dash.example.local/foo/*", "http://localhost:8280/foo/*")
                .secret("password");
        realm.client(installedApp3);

        ClientBuilder installedApp4 = ClientBuilder.create().id("test-dash").name("test-dash")
                .redirectUris("http://with-dash.example.local", "http://with-dash.example.local/foo")
                .secret("password");
        realm.client(installedApp4);

        ClientBuilder installedApp5 = ClientBuilder.create().id("test-root-url").name("test-root-url")
                .rootUrl("http://with-dash.example.local")
                .redirectUris("/foo")
                .secret("password");
        realm.client(installedApp5);

        ClientBuilder installedApp6 = ClientBuilder.create().id("test-relative-url").name("test-relative-url")
                .rootUrl("")
                .redirectUris("/auth")
                .secret("password");
        realm.client(installedApp6);

        ClientBuilder installedApp7 = ClientBuilder.create().id("test-query-component").name("test-query-component")
                .redirectUris("http://localhost?foo=bar", "http://localhost?foo=bar*")
                .secret("password");
        realm.client(installedApp7);

        ClientBuilder installedApp8 = ClientBuilder.create().id("test-fragment").name("test-fragment")
                .redirectUris("http://localhost:8180/*", "https://localhost:8543/*")
                .secret("password");
        realm.client(installedApp8);

        ClientBuilder installedAppCustomScheme = ClientBuilder.create().id("custom-scheme").name("custom-scheme")
                .redirectUris("android-app://org.keycloak.examples.cordova/https/keycloak-cordova-example.github.io/login")
                .secret("password");
        realm.client(installedAppCustomScheme);

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
    public void testRelativeUri() throws IOException {
        oauth.redirectUri("/foo/../bar");
        oauth.openLoginForm();
        Assert.assertTrue(errorPage.isCurrent());
        Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
    }

    @Test
    public void testFileUri() throws IOException {
        oauth.redirectUri("file://test");
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
    public void testWithFragment() throws IOException {
        oauth.clientId("test-fragment");
        oauth.responseMode("fragment");

        oauth.redirectUri(APP_ROOT + "/auth#key=value");
        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        URL url = new URL(driver.getCurrentUrl());
        Assert.assertTrue(url.toString().startsWith(APP_ROOT));
        Assert.assertTrue(url.toString().contains("key=value"));
    }

    @Test
    public void testWithCustomScheme() throws IOException {
        oauth.clientId("custom-scheme");

        oauth.redirectUri("android-app://org.keycloak.examples.cordova/https/keycloak-cordova-example.github.io/login");
        oauth.openLoginForm();

        RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.BEST_MATCH).build();
        CookieStore cookieStore = new BasicCookieStore();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        String loginUrl = driver.getCurrentUrl();

        CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();

        try {
            String loginPage = SimpleHttp.doGet(loginUrl, client).asString();

            String formAction = loginPage.split("action=\"")[1].split("\"")[0].replaceAll("&amp;", "&");
            SimpleHttp.Response response = SimpleHttp.doPost(formAction, client).param("username", "test-user@localhost").param("password", "password").asResponse();

            response.getStatus();
            assertThat(response.getFirstHeader("Location"), Matchers.startsWith("android-app://org.keycloak.examples.cordova/https/keycloak-cordova-example.github.io/login"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testQueryComponents() throws IOException {
        // KEYCLOAK-3420
        oauth.clientId("test-query-component");
        checkRedirectUri("http://localhost?foo=bar", true);
        checkRedirectUri("http://localhost?foo=bara", false);
        checkRedirectUri("http://localhost?foo=bar/", false);
        checkRedirectUri("http://localhost?foo2=bar2&foo=bar", false);
        checkRedirectUri("http://localhost?foo=b", false);
        checkRedirectUri("http://localhost?foo", false);
        checkRedirectUri("http://localhost?foo=bar&bar=foo", false);
        checkRedirectUri("http://localhost?foo&bar=foo", false);
        checkRedirectUri("http://localhost?foo&bar", false);
        checkRedirectUri("http://localhost", false);

        // KEYCLOAK-3418
        oauth.clientId("test-installed");
        checkRedirectUri("http://localhost?foo=bar", false);
    }

    @Test
    public void testWildcard() throws IOException {
        oauth.clientId("test-wildcard");
        checkRedirectUri("http://example.com", false);
        checkRedirectUri("http://localhost:8080", false, true);
        checkRedirectUri("http://example.com/foo", true);
        checkRedirectUri("http://example.com/foo/bar", true);
        checkRedirectUri("http://localhost:8280/foo", true, true);
        checkRedirectUri("http://localhost:8280/foo/bar", true, true);
        checkRedirectUri("http://example.com/foobar", false);
        checkRedirectUri("http://localhost:8280/foobar", false, true);
    }

    @Test
    public void testDash() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("http://with-dash.example.local/foo", true);
    }

    @Test
    public void testDifferentCaseInHostname() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("http://with-dash.example.local", true);
        checkRedirectUri("http://wiTh-dAsh.example.local", true);
        checkRedirectUri("http://with-dash.example.local/foo", true);
        checkRedirectUri("http://wiTh-dAsh.example.local/foo", true);
        checkRedirectUri("http://with-dash.example.local/foo", true);
        checkRedirectUri("http://wiTh-dAsh.example.local/foo", true);
        checkRedirectUri("http://wiTh-dAsh.example.local/Foo", false);
        checkRedirectUri("http://wiTh-dAsh.example.local/foO", false);
    }

    @Test
    public void testDifferentCaseInScheme() throws IOException {
        oauth.clientId("test-dash");

        checkRedirectUri("HTTP://with-dash.example.local", true);
        checkRedirectUri("Http://wiTh-dAsh.example.local", true);
    }

    @Test
    public void testRelativeWithRoot() throws IOException {
        oauth.clientId("test-root-url");

        checkRedirectUri("http://with-dash.example.local/foo", true);
        checkRedirectUri("http://localhost:8180/foo", false);
    }

    @Test
    public void testRelative() throws IOException {
        oauth.clientId("test-relative-url");

        checkRedirectUri("http://with-dash.example.local/foo", false);
        checkRedirectUri("http://localhost:8180/auth", true);
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

    @Test
    public void okThenNull() throws IOException {
        oauth.clientId("test-wildcard");
        oauth.redirectUri("http://localhost:8280/foo");
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        Assert.assertNotNull(code);
        oauth.redirectUri(null);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals("Expected 400, but got something else", 400, tokenResponse.getStatusCode());
    }

    private void checkRedirectUri(String redirectUri, boolean expectValid) throws IOException {
        checkRedirectUri(redirectUri, expectValid, false);
    }

    private void checkRedirectUri(String redirectUri, boolean expectValid, boolean checkCodeToToken) throws IOException {
        oauth.redirectUri(redirectUri);

        if (!expectValid) {
            oauth.openLoginForm();
            Assert.assertTrue(errorPage.isCurrent());
            Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        } else {
            if (!checkCodeToToken) {
                oauth.openLoginForm();
                Assert.assertTrue(loginPage.isCurrent());
            } else {
                oauth.doLogin("test-user@localhost", "password");

                String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
                Assert.assertNotNull(code);

                OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

                Assert.assertEquals("Expected success, but got error: " + tokenResponse.getError(), 200, tokenResponse.getStatusCode());

                oauth.doLogout(tokenResponse.getRefreshToken(), "password");
            }
        }
    }

}
