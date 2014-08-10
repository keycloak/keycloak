/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.adapter;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.Version;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.TokenService;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testutils.KeycloakServer;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.security.PublicKey;
import java.util.Map;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class AdapterTest {

    public static final String LOGIN_URL = TokenService.loginPageUrl(UriBuilder.fromUri("http://localhost:8081/auth")).build("demo").toString();
    public static PublicKey realmPublicKey;
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            RealmRepresentation representation = KeycloakServer.loadJson(getClass().getResourceAsStream("/adapter-test/demorealm.json"), RealmRepresentation.class);
            RealmModel realm = manager.importRealm(representation);

            realmPublicKey = realm.getPublicKey();

            URL url = getClass().getResource("/adapter-test/cust-app-keycloak.json");
            deployApplication("customer-portal", "/customer-portal", CustomerServlet.class, url.getPath(), "user");
            url = getClass().getResource("/adapter-test/secure-portal-keycloak.json");
            deployApplication("secure-portal", "/secure-portal", CallAuthenticatedServlet.class, url.getPath(), "user", false);
            url = getClass().getResource("/adapter-test/customer-db-keycloak.json");
            deployApplication("customer-db", "/customer-db", CustomerDatabaseServlet.class, url.getPath(), "user");
            url = getClass().getResource("/adapter-test/product-keycloak.json");
            deployApplication("product-portal", "/product-portal", ProductServlet.class, url.getPath(), "user");

        }
    };

    private static String createToken() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminRealm = manager.getRealm(Config.getAdminRealm());
            ApplicationModel adminConsole = adminRealm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
            TokenManager tm = new TokenManager();
            UserModel admin = session.users().getUserByUsername("admin", adminRealm);
            UserSessionModel userSession = session.sessions().createUserSession(adminRealm, admin, "admin", null, "form", false);
            AccessToken token = tm.createClientAccessToken(tm.getAccess(null, adminConsole, admin), adminRealm, adminConsole, admin, userSession);
            return tm.encodeToken(adminRealm, token);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @Test
    public void testLoginSSOAndLogout() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to("http://localhost:8081/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/product-portal");
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // View stats
        String adminToken = createToken();

        Client client = ClientBuilder.newClient();
        UriBuilder authBase = UriBuilder.fromUri("http://localhost:8081/auth");
        WebTarget adminTarget = client.target(AdminRoot.realmsUrl(authBase)).path("demo");
        Map<String, SessionStats> stats = adminTarget.path("session-stats").request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .get(new GenericType<Map<String, SessionStats>>() {
                });

        SessionStats custStats = stats.get("customer-portal");
        Assert.assertNotNull(custStats);
        Assert.assertEquals(1, custStats.getActiveSessions());
        SessionStats prodStats = stats.get("product-portal");
        Assert.assertNotNull(prodStats);
        Assert.assertEquals(1, prodStats.getActiveSessions());

        client.close();


        // test logout

        String logoutUri = TokenService.logoutUrl(UriBuilder.fromUri("http://localhost:8081/auth"))
                .queryParam(OAuth2Constants.REDIRECT_URI, "http://localhost:8081/customer-portal").build("demo").toString();
        driver.navigate().to(logoutUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to("http://localhost:8081/customer-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));


    }

    @Test
    public void testServletRequestLogout() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to("http://localhost:8081/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/product-portal");
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // back
        driver.navigate().to("http://localhost:8081/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
        pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
        // test logout

        driver.navigate().to("http://localhost:8081/customer-portal/logout");



        driver.navigate().to("http://localhost:8081/customer-portal");
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));


    }

    @Test
    public void testLoginSSOIdle() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to("http://localhost:8081/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("demo");
        int originalIdle = realm.getSsoSessionIdleTimeout();
        realm.setSsoSessionIdleTimeout(1);
        session.getTransaction().commit();
        session.close();

        Thread.sleep(2000);


        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("demo");
        realm.setSsoSessionIdleTimeout(originalIdle);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testLoginSSOIdleRemoveExpiredUserSessions() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to("http://localhost:8081/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("demo");
        int originalIdle = realm.getSsoSessionIdleTimeout();
        realm.setSsoSessionIdleTimeout(1);
        session.getTransaction().commit();
        session.close();

        Thread.sleep(2000);

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("demo");
        session.sessions().removeExpiredUserSessions(realm);
        session.getTransaction().commit();
        session.close();

        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("demo");
        realm.setSsoSessionIdleTimeout(originalIdle);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testLoginSSOMax() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to("http://localhost:8081/customer-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("demo");
        int original = realm.getSsoSessionMaxLifespan();
        realm.setSsoSessionMaxLifespan(1);
        session.getTransaction().commit();
        session.close();

        Thread.sleep(2000);


        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("demo");
        realm.setSsoSessionMaxLifespan(original);
        session.getTransaction().commit();
        session.close();
    }

    /**
     * KEYCLOAK-518
     * @throws Exception
     */
    @Test
    public void testNullBearerToken() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081/customer-db");
        Response response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();
        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();
        client.close();

    }

    /**
     * KEYCLOAK-518
     * @throws Exception
     */
    @Test
    public void testBadUser() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        URI uri = TokenService.grantAccessTokenUrl(builder).build("demo");
        WebTarget target = client.target(uri);
        String header = BasicAuthHelper.createHeader("customer-portal", "password");
        Form form = new Form();
        form.param("username", "monkey@redhat.com")
            .param("password", "password");
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
        Assert.assertEquals(400, response.getStatus());
        response.close();
        client.close();

    }

    @Test
    public void testVersion() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT).path("version");
        Version version = target.request().get(Version.class);
        Assert.assertNotNull(version);
        Assert.assertNotNull(version.getVersion());
        Assert.assertNotNull(version.getBuildTime());
        Assert.assertNotEquals(version.getVersion(), Version.UNKNOWN);
        Assert.assertNotEquals(version.getBuildTime(), Version.UNKNOWN);

        Version version2 = client.target("http://localhost:8081/secure-portal").path(AdapterConstants.K_VERSION).request().get(Version.class);
        Assert.assertNotNull(version2);
        Assert.assertNotNull(version2.getVersion());
        Assert.assertNotNull(version2.getBuildTime());
        Assert.assertEquals(version.getVersion(), version2.getVersion());
        Assert.assertEquals(version.getBuildTime(), version2.getBuildTime());
        client.close();

    }



    @Test
    public void testAuthenticated() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to("http://localhost:8081/secure-portal");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/secure-portal");
        String pageSource = driver.getPageSource();
        System.out.println(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test logout

        String logoutUri = TokenService.logoutUrl(UriBuilder.fromUri("http://localhost:8081/auth"))
                .queryParam(OAuth2Constants.REDIRECT_URI, "http://localhost:8081/secure-portal").build("demo").toString();
        driver.navigate().to(logoutUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to("http://localhost:8081/secure-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
    }



}
