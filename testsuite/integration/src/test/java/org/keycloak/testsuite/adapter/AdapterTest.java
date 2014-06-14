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
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.cache.CacheKeycloakSession;
import org.keycloak.provider.ProviderSession;
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
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
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
        protected void configure(RealmManager manager, RealmModel adminRealm) {
            RealmRepresentation representation = KeycloakServer.loadJson(getClass().getResourceAsStream("/adapter-test/demorealm.json"), RealmRepresentation.class);
            RealmModel realm = manager.importRealm(representation);

            realmPublicKey = realm.getPublicKey();

            URL url = getClass().getResource("/adapter-test/cust-app-keycloak.json");
            deployApplication("customer-portal", "/customer-portal", CustomerServlet.class, url.getPath(), "user");
            url = getClass().getResource("/adapter-test/customer-db-keycloak.json");
            deployApplication("customer-db", "/customer-db", CustomerDatabaseServlet.class, url.getPath(), "user");
            url = getClass().getResource("/adapter-test/product-keycloak.json");
            deployApplication("product-portal", "/product-portal", ProductServlet.class, url.getPath(), "user");

        }
    };

    private static String createToken() {
        ProviderSession providerSession = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(providerSession.getProvider(KeycloakSession.class));

            RealmModel adminRealm = manager.getRealm(Config.getAdminRealm());
            ApplicationModel adminConsole = adminRealm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
            TokenManager tm = new TokenManager();
            UserModel admin = adminRealm.getUser("admin");
            UserSessionModel userSession = adminRealm.createUserSession(admin, null);
            AccessToken token = tm.createClientAccessToken(null, adminRealm, adminConsole, admin, userSession);
            return tm.encodeToken(adminRealm, token);
        } finally {
            keycloakRule.stopSession(providerSession, true);

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

        KeycloakSession keycloakSession = keycloakRule.startCacheSession();
        RealmModel realm = keycloakSession.getRealmByName("demo");
        int originalIdle = realm.getSsoSessionIdleTimeout();
        realm.setSsoSessionIdleTimeout(1);
        keycloakSession.getTransaction().commit();
        keycloakSession.close();

        Thread.sleep(2000);


        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        keycloakSession = keycloakRule.startCacheSession();
        realm = keycloakSession.getRealmByName("demo");
        realm.setSsoSessionIdleTimeout(originalIdle);
        keycloakSession.getTransaction().commit();
        keycloakSession.close();
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

        KeycloakSession keycloakSession = keycloakRule.startCacheSession();
        RealmModel realm = keycloakSession.getRealmByName("demo");
        int originalIdle = realm.getSsoSessionIdleTimeout();
        realm.setSsoSessionIdleTimeout(1);
        keycloakSession.getTransaction().commit();
        keycloakSession.close();

        Thread.sleep(2000);

        keycloakSession = keycloakRule.startCacheSession();
        realm = keycloakSession.getRealmByName("demo");
        realm.removeExpiredUserSessions();
        keycloakSession.getTransaction().commit();
        keycloakSession.close();

        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        keycloakSession = keycloakRule.startCacheSession();
        realm = keycloakSession.getRealmByName("demo");
        realm.setSsoSessionIdleTimeout(originalIdle);
        keycloakSession.getTransaction().commit();
        keycloakSession.close();
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

        KeycloakSession keycloakSession = keycloakRule.startCacheSession();
        RealmModel realm = keycloakSession.getRealmByName("demo");
        int original = realm.getSsoSessionMaxLifespan();
        realm.setSsoSessionMaxLifespan(1);
        keycloakSession.getTransaction().commit();
        keycloakSession.close();

        Thread.sleep(2000);


        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));

        keycloakSession = keycloakRule.startCacheSession();
        realm = keycloakSession.getRealmByName("demo");
        realm.setSsoSessionMaxLifespan(original);
        keycloakSession.getTransaction().commit();
        keycloakSession.close();
    }
}
