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
package org.keycloak.testsuite.adapter;

import org.apache.http.conn.params.ConnManagerParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.OIDCAuthenticationError;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Version;
import org.keycloak.common.util.Time;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.VersionRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountSessionsPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.*;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class AdapterTestStrategy extends ExternalResource {

    protected String AUTH_SERVER_URL = "http://localhost:8081/auth";
    protected String APP_SERVER_BASE_URL = "http://localhost:8081";
    protected AbstractKeycloakRule keycloakRule;
    // some servlet containers redirect to root + "/" if you visit root context
    protected String slash = "";

    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected InputPage inputPage;

    @WebResource
    protected AccountSessionsPage accountSessionsPage;

    protected String LOGIN_URL = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(AUTH_SERVER_URL)).build("demo").toString();

    public AdapterTestStrategy(String AUTH_SERVER_URL, String APP_SERVER_BASE_URL, AbstractKeycloakRule keycloakRule) {
        this.AUTH_SERVER_URL = AUTH_SERVER_URL;
        this.APP_SERVER_BASE_URL = APP_SERVER_BASE_URL;
        this.keycloakRule = keycloakRule;
    }

    public AdapterTestStrategy(String AUTH_SERVER_URL, String APP_SERVER_BASE_URL, AbstractKeycloakRule keycloakRule, boolean addSlash) {
        this.AUTH_SERVER_URL = AUTH_SERVER_URL;
        this.APP_SERVER_BASE_URL = APP_SERVER_BASE_URL;
        this.keycloakRule = keycloakRule;
        // some servlet containers redirect to root + "/" if you visit root context
        if (addSlash) slash = "/";
    }

    public static RealmModel baseAdapterTestInitialization(KeycloakSession session, RealmManager manager, RealmModel adminRealm, Class<?> clazz) {
        RealmRepresentation representation = KeycloakServer.loadJson(clazz.getResourceAsStream("/adapter-test/demorealm.json"), RealmRepresentation.class);
        RealmModel demoRealm = manager.importRealm(representation);
        return demoRealm;
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        webRule.before();
    }

    @Override
    protected void after() {
        super.after();
        webRule.after();
    }

    public void testSavedPostRequest() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/input-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/input-portal" + slash);
        inputPage.execute("hello");

        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/input-portal/secured/post");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("parameter=hello"));
        // test that user principal and KeycloakSecurityContext available
        driver.navigate().to(APP_SERVER_BASE_URL + "/input-portal/insecure");
        System.out.println("insecure: ");
        System.out.println(driver.getPageSource());
        Assert.assertTrue(driver.getPageSource().contains("Insecure Page"));
        if (System.getProperty("insecure.user.principal.unsupported") == null)
            Assert.assertTrue(driver.getPageSource().contains("UserPrincipal"));

        // test logout

        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
                .queryParam(OAuth2Constants.REDIRECT_URI, APP_SERVER_BASE_URL + "/customer-portal").build("demo").toString();
        driver.navigate().to(logoutUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        // test unsecured POST KEYCLOAK-901

        Client client = ClientBuilder.newClient();
        Form form = new Form();
        form.param("parameter", "hello");
        String text = client.target(APP_SERVER_BASE_URL + "/input-portal/unsecured").request().post(Entity.form(form), String.class);
        Assert.assertTrue(text.contains("parameter=hello"));
        client.close();

    }


    public void testLoginSSOAndLogout() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/product-portal" + slash);
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // View stats
        List<Map<String, String>> stats = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID).realm("demo").getClientSessionStats();
        Map<String, String> customerPortalStats = null;
        Map<String, String> productPortalStats = null;
        for (Map<String, String> s : stats) {
            if (s.get("clientId").equals("customer-portal")) {
                customerPortalStats = s;
            } else if (s.get("clientId").equals("product-portal")) {
                productPortalStats = s;
            }
        }
        Assert.assertEquals(1, Integer.parseInt(customerPortalStats.get("active")));
        Assert.assertEquals(1, Integer.parseInt(productPortalStats.get("active")));

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
                .queryParam(OAuth2Constants.REDIRECT_URI, APP_SERVER_BASE_URL + "/customer-portal").build("demo").toString();
        driver.navigate().to(logoutUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
    }

    public void testServletRequestLogout() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/product-portal" + slash);
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // back
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
        // test logout

        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal/logout");
        Assert.assertTrue(driver.getPageSource().contains("servlet logout ok"));


        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));


    }

    public void testLoginSSOIdle() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("demo");
        int originalIdle = realm.getSsoSessionIdleTimeout();
        realm.setSsoSessionIdleTimeout(1);
        session.getTransactionManager().commit();
        session.close();

        Time.setOffset(2);


        // test SSO
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("demo");
        realm.setSsoSessionIdleTimeout(originalIdle);
        session.getTransactionManager().commit();
        session.close();

        Time.setOffset(0);
    }

    public void testLoginSSOIdleRemoveExpiredUserSessions() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("demo");
        int originalIdle = realm.getSsoSessionIdleTimeout();
        realm.setSsoSessionIdleTimeout(1);
        session.getTransactionManager().commit();
        session.close();

        Time.setOffset(2);

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("demo");
        session.sessions().removeExpired(realm);
        session.getTransactionManager().commit();
        session.close();

        // test SSO
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("demo");
        // need to cleanup so other tests don't fail, so invalidate http sessions on remote clients.
        UserModel user = session.users().getUserByUsername("bburke@redhat.com", realm);
        new ResourceAdminManager(session).logoutUser(null, realm, user, session);
        realm.setSsoSessionIdleTimeout(originalIdle);
        session.getTransactionManager().commit();
        session.close();

        Time.setOffset(0);
    }

    public void testLoginSSOMax() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/customer-portal" + slash);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("demo");
        int original = realm.getSsoSessionMaxLifespan();
        realm.setSsoSessionMaxLifespan(1);
        session.getTransactionManager().commit();
        session.close();

        Time.setOffset(2);


        // test SSO
        driver.navigate().to(APP_SERVER_BASE_URL + "/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("demo");
        realm.setSsoSessionMaxLifespan(original);
        session.getTransactionManager().commit();
        session.close();

        Time.setOffset(0);
    }

    /**
     * KEYCLOAK-518
     * @throws Exception
     */
    public void testNullBearerToken() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(APP_SERVER_BASE_URL + "/customer-db/");
        Response response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();
        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();
        client.close();

    }

    /**
     * KEYCLOAK-1733
     *
     * @throws Exception
     */
    public void testNullQueryParameterAccessToken() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(APP_SERVER_BASE_URL + "/customer-db/");
        Response response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        target = client.target(APP_SERVER_BASE_URL + "/customer-db?access_token=");
        response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        client.close();
    }

    /**
     * KEYCLOAK-1368
     * @throws Exception
     */
    public void testNullBearerTokenCustomErrorPage() throws Exception {
        ErrorServlet.authError = null;
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(APP_SERVER_BASE_URL + "/customer-db-error-page/");

        Response response = target.request().get();

        // TODO: follow redirects automatically if possible
        if (response.getStatus() == 302) {
            String location = response.getHeaderString(HttpHeaders.LOCATION);
            response.close();
            response = client.target(location).request().get();
        }
        Assert.assertEquals(401, response.getStatus());
        String errorPageResponse = response.readEntity(String.class);
        Assert.assertTrue(errorPageResponse.contains("Error Page"));
        response.close();
        Assert.assertNotNull(ErrorServlet.authError);
        OIDCAuthenticationError error = (OIDCAuthenticationError) ErrorServlet.authError;
        Assert.assertEquals(OIDCAuthenticationError.Reason.NO_BEARER_TOKEN, error.getReason());

        ErrorServlet.authError = null;
        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        // TODO: follow redirects automatically if possible
        if (response.getStatus() == 302) {
            String location = response.getHeaderString(HttpHeaders.LOCATION);
            response.close();
            response = client.target(location).request().get();
        }
        Assert.assertEquals(401, response.getStatus());
        errorPageResponse = response.readEntity(String.class);
        Assert.assertTrue(errorPageResponse.contains("Error Page"));
        response.close();
        Assert.assertNotNull(ErrorServlet.authError);
        error = (OIDCAuthenticationError) ErrorServlet.authError;
        Assert.assertEquals(OIDCAuthenticationError.Reason.INVALID_TOKEN, error.getReason());

        client.close();

    }

    /**
     * KEYCLOAK-3016
     * @throws Exception
     */
    public void testBasicAuthErrorHandling() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(APP_SERVER_BASE_URL + "/customer-db/");
        Response response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // The number of iterations should be HttpClient's connection pool size + 1.
        final int LIMIT = ConnManagerParams.DEFAULT_MAX_TOTAL_CONNECTIONS + 1;
        for (int i = 0; i < LIMIT; i++) {
            System.out.println("Testing Basic Auth with bad credentials " + i);
            response = target.request().header(HttpHeaders.AUTHORIZATION, "Basic dXNlcm5hbWU6cGFzc3dvcmQ=").get();
            Assert.assertEquals(401, response.getStatus());
            response.close();
        }

        client.close();
    }

    /**
     * KEYCLOAK-518
     * @throws Exception
     */
    public void testBadUser() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_URL);
        URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("demo");
        WebTarget target = client.target(uri);
        String header = BasicAuthHelper.createHeader("customer-portal", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "monkey@redhat.com")
                .param("password", "password");
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
        Assert.assertEquals(401, response.getStatus());
        response.close();
        client.close();

    }

    public void testVersion() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(AUTH_SERVER_URL).path("version");
        VersionRepresentation version = target.request().get(VersionRepresentation.class);
        Assert.assertNotNull(version);
        Assert.assertNotNull(version.getVersion());
        Assert.assertNotNull(version.getBuildTime());
        Assert.assertNotEquals(version.getVersion(), Version.UNKNOWN);
        Assert.assertNotEquals(version.getBuildTime(), Version.UNKNOWN);

        VersionRepresentation version2 = client.target(APP_SERVER_BASE_URL + "/secure-portal").path(AdapterConstants.K_VERSION).request().get(VersionRepresentation.class);
        Assert.assertNotNull(version2);
        Assert.assertNotNull(version2.getVersion());
        Assert.assertNotNull(version2.getBuildTime());
        Assert.assertEquals(version.getVersion(), version2.getVersion());
        Assert.assertEquals(version.getBuildTime(), version2.getBuildTime());
        client.close();

    }


    public void testAuthenticated() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(APP_SERVER_BASE_URL + "/secure-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/secure-portal" + slash);
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test logout

        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
                .queryParam(OAuth2Constants.REDIRECT_URI, APP_SERVER_BASE_URL + "/secure-portal").build("demo").toString();
        driver.navigate().to(logoutUri);
        String currentUrl = driver.getCurrentUrl();
        pageSource = driver.getPageSource();
        Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));
        driver.navigate().to(APP_SERVER_BASE_URL + "/secure-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
    }

    /**
     * KEYCLOAK-1733
     *
     * @throws Exception
     */
    public void testRestCallWithAccessTokenAsQueryParameter() throws Exception {
        String accessToken = getAccessToken();
        Client client = ClientBuilder.newClient();
        try {
            // test without token
            Response response = client.target(APP_SERVER_BASE_URL + "/customer-db").request().get();
            Assert.assertEquals(401, response.getStatus());
            response.close();
            // test with access_token as QueryParamter
            response = client.target(APP_SERVER_BASE_URL + "/customer-db").queryParam("access_token", accessToken).request().get();
            Assert.assertEquals(200, response.getStatus());
            response.close();
        } finally {
            client.close();
        }
    }

    private String getAccessToken() throws JSONException {
        String tokenUrl = AUTH_SERVER_URL + "/realms/demo/protocol/openid-connect/token";

        Client client = ClientBuilder.newClient();
        try {
            WebTarget webTarget = client.target(tokenUrl);

            Form form = new Form();
            form.param("grant_type", "password");
            form.param("client_id", "customer-portal-public");
            form.param("username", "bburke@redhat.com");
            form.param("password", "password");
            Response response = webTarget.request().post(Entity.form(form));

            Assert.assertEquals(200, response.getStatus());

            JSONObject jsonObject = new JSONObject(response.readEntity(String.class));
            System.out.println(jsonObject);
            response.close();
            return jsonObject.getString("access_token");
        } finally {
            client.close();
        }
    }

    /**
     * KEYCLOAK-732
     *
     * @throws Throwable
     */
    public void testSingleSessionInvalidated() throws Throwable {
        AdapterTestStrategy browser1 = this;
        AdapterTestStrategy browser2 = new AdapterTestStrategy(AUTH_SERVER_URL, APP_SERVER_BASE_URL, keycloakRule);

        loginAndCheckSession(browser1.driver, browser1.loginPage);

        // Open browser2
        browser2.webRule.before();
        try {
            loginAndCheckSession(browser2.driver, browser2.loginPage);

            // Logout in browser1
            String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
                    .queryParam(OAuth2Constants.REDIRECT_URI, APP_SERVER_BASE_URL + "/session-portal").build("demo").toString();
            browser1.driver.navigate().to(logoutUri);
            Assert.assertTrue(browser1.driver.getCurrentUrl().startsWith(LOGIN_URL));

            // Assert that I am logged out in browser1
            browser1.driver.navigate().to(APP_SERVER_BASE_URL + "/session-portal");
            Assert.assertTrue(browser1.driver.getCurrentUrl().startsWith(LOGIN_URL));

            // Assert that I am still logged in browser2 and same session is still preserved
            browser2.driver.navigate().to(APP_SERVER_BASE_URL + "/session-portal");
            Assert.assertEquals(browser2.driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/session-portal" + slash);
            String pageSource = browser2.driver.getPageSource();
            Assert.assertTrue(pageSource.contains("Counter=3"));

            browser2.driver.navigate().to(logoutUri);
            Assert.assertTrue(browser2.driver.getCurrentUrl().startsWith(LOGIN_URL));
        } finally {
            browser2.webRule.after();
        }
    }

    /**
     * KEYCLOAK-741
     */
    public void testSessionInvalidatedAfterFailedRefresh() throws Throwable {
        final AtomicInteger origTokenLifespan = new AtomicInteger();

        // Delete adminUrl and set short accessTokenLifespan
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel demoRealm) {
                ClientModel sessionPortal = demoRealm.getClientByClientId("session-portal");
                sessionPortal.setManagementUrl(null);

                origTokenLifespan.set(demoRealm.getAccessTokenLifespan());
                demoRealm.setAccessTokenLifespan(1);
            }
        }, "demo");

        // Login
        loginAndCheckSession(driver, loginPage);

        // Logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
                .queryParam(OAuth2Constants.REDIRECT_URI, APP_SERVER_BASE_URL + "/session-portal").build("demo").toString();
        driver.navigate().to(logoutUri);

        // Wait until accessToken is expired
        Time.setOffset(2);

        // Assert that http session was invalidated
        driver.navigate().to(APP_SERVER_BASE_URL + "/session-portal");
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/session-portal" + slash);
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Counter=1"));

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel demoRealm) {
                ClientModel sessionPortal = demoRealm.getClientByClientId("session-portal");
                sessionPortal.setManagementUrl(APP_SERVER_BASE_URL + "/session-portal");

                demoRealm.setAccessTokenLifespan(origTokenLifespan.get());
            }

        }, "demo");

        Time.setOffset(0);
    }

    /**
     * KEYCLOAK-942
     */
    public void testAdminApplicationLogout() throws Throwable {
        // login as bburke
        loginAndCheckSession(driver, loginPage);

        // logout mposolda with admin client
        Keycloak keycloakAdmin = Keycloak.getInstance(AUTH_SERVER_URL, "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID);
        UserRepresentation mposolda = keycloakAdmin.realm("demo").users().search("mposolda", null, null, null, null, null).get(0);
        keycloakAdmin.realm("demo").users().get(mposolda.getId()).logout();

        // bburke should be still logged with original httpSession in our browser window
        driver.navigate().to(APP_SERVER_BASE_URL + "/session-portal");
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/session-portal" + slash);
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Counter=3"));
    }

    /**
     * KEYCLOAK-1216
     */
    public void testAccountManagementSessionsLogout() throws Throwable {
        // login as bburke
        loginAndCheckSession(driver, loginPage);

        // logout sessions in account management
        accountSessionsPage.realm("demo");
        accountSessionsPage.open();
        Assert.assertTrue(accountSessionsPage.isCurrent());
        accountSessionsPage.logoutAll();

        // Assert I need to login again (logout was propagated to the app)
        loginAndCheckSession(driver, loginPage);
    }

    protected void loginAndCheckSession(WebDriver driver, LoginPage loginPage) {
        driver.navigate().to(APP_SERVER_BASE_URL + "/session-portal");
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), APP_SERVER_BASE_URL + "/session-portal" + slash);
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Counter=1"));

        // Counter increased now
        driver.navigate().to(APP_SERVER_BASE_URL + "/session-portal");
        pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Counter=2"));

    }

}
