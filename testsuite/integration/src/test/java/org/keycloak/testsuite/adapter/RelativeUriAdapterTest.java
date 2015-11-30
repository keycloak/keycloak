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
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testsuite.KeycloakServer;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

/**
 * Tests Undertow Adapter
 *
 * Also tests relative URIs in the adapter and valid redirect uris.
 * Also tests adapters not configured with public key
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class RelativeUriAdapterTest {

    public static final String LOGIN_URL = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri("http://localhost:8081/auth")).build("demo").toString();
    public static PublicKey realmPublicKey;
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule(){
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            RealmRepresentation representation = KeycloakServer.loadJson(getClass().getResourceAsStream("/adapter-test/demorealm-relative.json"), RealmRepresentation.class);
            RealmModel realm = manager.importRealm(representation);

            realmPublicKey = realm.getPublicKey();

            URL url = getClass().getResource("/adapter-test/cust-app-keycloak-relative.json");
            createApplicationDeployment()
                    .name("customer-portal").contextPath("/customer-portal")
                    .servletClass(CustomerServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();

            url = getClass().getResource("/adapter-test/customer-db-keycloak-relative.json");
            createApplicationDeployment()
                    .name("customer-db").contextPath("/customer-db")
                    .servletClass(CustomerDatabaseServlet.class).adapterConfigPath(url.getPath())
                    .role("user")
                    .errorPage(null).deployApplication();

            url = getClass().getResource("/adapter-test/product-keycloak-relative.json");
            createApplicationDeployment()
                    .name("product-portal").contextPath("/product-portal")
                    .servletClass(ProductServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();
        }
    };

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
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/product-portal");
        pageSource = driver.getPageSource();
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
        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri("http://localhost:8081/auth"))
                .queryParam(OAuth2Constants.REDIRECT_URI, "/customer-portal").build("demo").toString();
        driver.navigate().to(logoutUri);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        driver.navigate().to("http://localhost:8081/customer-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
    }

    @Test
    public void testServletRequestLogout() throws Exception {
        driver.navigate().to("http://localhost:8081/customer-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
        loginPage.login("bburke@redhat.com", "password");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/customer-portal");
        Assert.assertTrue(driver.getPageSource().contains("Bill Burke"));

        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getPageSource().contains("iPhone"));

        // test logout
        driver.navigate().to("http://localhost:8081/customer-portal/logout");
        Assert.assertTrue(driver.getPageSource().contains("servlet logout ok"));

        driver.navigate().to("http://localhost:8081/customer-portal");
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(LOGIN_URL));
        driver.navigate().to("http://localhost:8081/product-portal");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(LOGIN_URL));
    }

}
