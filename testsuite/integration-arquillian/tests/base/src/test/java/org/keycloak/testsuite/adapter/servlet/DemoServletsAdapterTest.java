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
package org.keycloak.testsuite.adapter.servlet;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.OIDCAuthenticationError;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Time;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.BasicAuth;
import org.keycloak.testsuite.adapter.page.ClientSecretJwtSecurePortal;
import org.keycloak.testsuite.adapter.page.ClientSecretJwtSecurePortalValidAlg;
import org.keycloak.testsuite.adapter.page.CustomerCookiePortal;
import org.keycloak.testsuite.adapter.page.CustomerCookiePortalRoot;
import org.keycloak.testsuite.adapter.page.CustomerDb;
import org.keycloak.testsuite.adapter.page.CustomerDbAudienceRequired;
import org.keycloak.testsuite.adapter.page.CustomerDbErrorPage;
import org.keycloak.testsuite.adapter.page.CustomerPortal;
import org.keycloak.testsuite.adapter.page.CustomerPortalNoConf;
import org.keycloak.testsuite.adapter.page.InputPortal;
import org.keycloak.testsuite.adapter.page.InputPortalNoAccessToken;
import org.keycloak.testsuite.adapter.page.ProductPortal;
import org.keycloak.testsuite.adapter.page.ProductPortalAutodetectBearerOnly;
import org.keycloak.testsuite.adapter.page.SecurePortal;
import org.keycloak.testsuite.adapter.page.SecurePortalRewriteRedirectUri;
import org.keycloak.testsuite.adapter.page.SecurePortalWithCustomSessionConfig;
import org.keycloak.testsuite.adapter.page.TokenMinTTLPage;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.console.page.events.Config;
import org.keycloak.testsuite.console.page.events.LoginEvents;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.testsuite.util.FollowRedirectsEngine;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT7)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT8)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT9)
public class DemoServletsAdapterTest extends AbstractServletsAdapterTest {

    // Javascript browser needed KEYCLOAK-4703
    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    @Page
    @JavascriptBrowser
    protected OIDCLogin jsDriverTestRealmLoginPage;

    @Page
    protected CustomerPortal customerPortal;
    @Page
    private CustomerPortalNoConf customerPortalNoConf;
    @Page
    private SecurePortal securePortal;
    @Page
    private SecurePortalWithCustomSessionConfig securePortalWithCustomSessionConfig;
    @Page
    private SecurePortalRewriteRedirectUri securePortalRewriteRedirectUri;
    @Page
    private CustomerDb customerDb;
    @Page
    private CustomerDbErrorPage customerDbErrorPage;
    @Page
    private ProductPortal productPortal;
    @Page
    private ProductPortalAutodetectBearerOnly productPortalAutodetectBearerOnly;
    @Page
    private InputPortal inputPortal;
    @Page
    private InputPortalNoAccessToken inputPortalNoAccessToken;
    @Page
    private TokenMinTTLPage tokenMinTTLPage;
    @Page
    private OAuthGrant oAuthGrantPage;
    @Page
    protected Applications applicationsPage;
    @Page
    protected LoginEvents loginEventsPage;
    @Page
    private BasicAuth basicAuthPage;
    @Page
    protected Config configPage;
    @Page
    private ClientSecretJwtSecurePortal clientSecretJwtSecurePortal;
    @Page
    private ClientSecretJwtSecurePortalValidAlg clientSecretJwtSecurePortalValidAlg;
    @Page
    private CustomerCookiePortal customerCookiePortal;
    @Page
    private CustomerCookiePortalRoot customerCookiePortalRoot;

    @Rule
    public AssertEvents assertEvents = new AssertEvents(this);

    @Deployment(name = CustomerPortal.DEPLOYMENT_NAME)
    protected static WebArchive customerPortal() {
        return servletDeployment(CustomerPortal.DEPLOYMENT_NAME, CustomerServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = CustomerCookiePortal.DEPLOYMENT_NAME)
    protected static WebArchive customerCookiePortal() {
        return servletDeployment(CustomerCookiePortal.DEPLOYMENT_NAME, AdapterActionsFilter.class, CustomerServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = CustomerPortalNoConf.DEPLOYMENT_NAME)
    protected static WebArchive customerPortalNoConf() {
        return servletDeployment(CustomerPortalNoConf.DEPLOYMENT_NAME, CustomerServletNoConf.class, ErrorServlet.class);
    }

    @Deployment(name = SecurePortal.DEPLOYMENT_NAME)
    protected static WebArchive securePortal() {
        return servletDeployment(SecurePortal.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }
    @Deployment(name = SecurePortalRewriteRedirectUri.DEPLOYMENT_NAME)
    protected static WebArchive securePortalRewriteRedirectUri() {
        return servletDeployment(SecurePortalRewriteRedirectUri.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }


    @Deployment(name = SecurePortalWithCustomSessionConfig.DEPLOYMENT_NAME)
    protected static WebArchive securePortalWithCustomSessionConfig() {
        return servletDeployment(SecurePortalWithCustomSessionConfig.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }

    @Deployment(name = CustomerDb.DEPLOYMENT_NAME)
    protected static WebArchive customerDb() {
        return servletDeployment(CustomerDb.DEPLOYMENT_NAME, AdapterActionsFilter.class, CustomerDatabaseServlet.class);
    }

    @Deployment(name = CustomerDbAudienceRequired.DEPLOYMENT_NAME)
    protected static WebArchive customerDbAudienceRequired() {
        return servletDeployment(CustomerDbAudienceRequired.DEPLOYMENT_NAME, AdapterActionsFilter.class, CustomerDatabaseServlet.class);
    }

    @Deployment(name = CustomerDbErrorPage.DEPLOYMENT_NAME)
    protected static WebArchive customerDbErrorPage() {
        return servletDeployment(CustomerDbErrorPage.DEPLOYMENT_NAME, CustomerDatabaseServlet.class, ErrorServlet.class);
    }

    @Deployment(name = ProductPortal.DEPLOYMENT_NAME)
    protected static WebArchive productPortal() {
        return servletDeployment(ProductPortal.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @Deployment(name = ProductPortalAutodetectBearerOnly.DEPLOYMENT_NAME)
    protected static WebArchive productPortalAutodetectBearerOnly() {
        return servletDeployment(ProductPortalAutodetectBearerOnly.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @Deployment(name = InputPortal.DEPLOYMENT_NAME)
    protected static WebArchive inputPortal() {
        return servletDeployment(InputPortal.DEPLOYMENT_NAME, "keycloak.json", InputServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = InputPortalNoAccessToken.DEPLOYMENT_NAME)
    protected static WebArchive inputPortalNoAccessToken() {
        return servletDeployment(InputPortalNoAccessToken.DEPLOYMENT_NAME, "keycloak.json", InputServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = TokenMinTTLPage.DEPLOYMENT_NAME)
    protected static WebArchive tokenMinTTLPage() {
        return servletDeployment(TokenMinTTLPage.DEPLOYMENT_NAME, AdapterActionsFilter.class, AbstractShowTokensServlet.class, TokenMinTTLServlet.class, ErrorServlet.class);
    }

    @Deployment(name = BasicAuth.DEPLOYMENT_NAME)
    protected static WebArchive basicAuth() {
        return servletDeployment(BasicAuth.DEPLOYMENT_NAME, BasicAuthServlet.class);
    }

    @Deployment(name = ClientSecretJwtSecurePortal.DEPLOYMENT_NAME)
    protected static WebArchive clientSecretSecurePortal() {
        return servletDeployment(ClientSecretJwtSecurePortal.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }

    @Deployment(name = ClientSecretJwtSecurePortalValidAlg.DEPLOYMENT_NAME)
    protected static WebArchive clientSecretSecurePortalValidAlg() {
        return servletDeployment(ClientSecretJwtSecurePortalValidAlg.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }

    @Deployment(name = CustomerCookiePortalRoot.DEPLOYMENT_NAME)
    protected static WebArchive customerCookiePortalRoot() {
        return servletDeployment(CustomerCookiePortalRoot.DEPLOYMENT_NAME, AdapterActionsFilter.class, CustomerServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        configPage.setConsoleRealm(DEMO);
        loginEventsPage.setConsoleRealm(DEMO);
        applicationsPage.setAuthRealm(DEMO);
        loginEventsPage.setConsoleRealm(DEMO);
        oAuthGrantPage.setAuthRealm(DEMO);
    }
    
    @Before
    public void beforeDemoServletsAdapterTest() {
        // Delete all cookies from token-min-ttl page to be sure we are logged out
        tokenMinTTLPage.navigateTo();
        driver.manage().deleteAllCookies();
    }
    
    //KEYCLOAK-702
    @Test
    public void testTokenInCookieSSO() {
        // Login
        String tokenCookie = loginToCustomerCookiePortal();
        
        // SSO to second app
        customerPortal.navigateTo();
        assertLogged();
        
        // return to customer-cookie-portal and assert still same cookie (accessToken didn't expire)
        customerCookiePortal.navigateTo();
        assertLogged();
        String tokenCookie2 = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
        assertEquals(tokenCookie, tokenCookie2);
        
        // Logout with httpServletRequest
        logoutFromCustomerCookiePortal();
        
        // Also should be logged-out from the second app
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }
    
    //KEYCLOAK-702
    @Test
    public void testTokenInCookieRefresh() {
        log.debug("Set token timeout 10 sec");
        RealmRepresentation demo = adminClient.realm("demo").toRepresentation();
        int originalTokenTimeout = demo.getAccessTokenLifespan();
        demo.setAccessTokenLifespan(10);
        adminClient.realm("demo").update(demo);

        try {
            log.debug("login to customer-cookie-portal");
            String tokenCookie1 = loginToCustomerCookiePortal();

            log.debug("Simulate waiting 12 seconds");
            setAdapterAndServerTimeOffset(12, customerCookiePortal.toString());

            log.debug("assert cookie was refreshed");
            customerCookiePortal.navigateTo();
            assertCurrentUrlEquals(customerCookiePortal);
            assertLogged();
            String tokenCookie2 = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
            assertNotEquals(tokenCookie1, tokenCookie2);
            
            log.debug("login to 2nd app and logout from it");
            customerPortal.navigateTo();
            assertCurrentUrlEquals(customerPortal);
            assertLogged();
            
            driver.navigate().to(customerPortal.logout().toASCIIString());
            WaitUtils.waitUntilElement(By.id("customer_portal_logout")).is().present();
            customerPortal.navigateTo();
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
            
            log.debug("Simulate another 12 seconds");
            setAdapterAndServerTimeOffset(24, customerCookiePortal.toString());
            
            log.debug("assert not logged in customer-cookie-portal");
            customerCookiePortal.navigateTo();
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        } finally {
            log.debug("Set token timeout to original");
            demo.setAccessTokenLifespan(originalTokenTimeout);
            adminClient.realm("demo").update(demo);
            
            log.debug("reset time offset");
            setAdapterAndServerTimeOffset(0, customerCookiePortal.toString().concat("/unsecured"));
        }
    }

    //KEYCLOAK-702
    @Test
    public void testInvalidTokenCookie() {
        // Login
        String tokenCookie = loginToCustomerCookiePortal();
        String changedTokenCookie = tokenCookie.replace("a", "b");
        
        // change cookie to invalid value
        driver.manage().addCookie(new Cookie(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE, changedTokenCookie, "/customer-cookie-portal"));
        
        // visit page and assert re-logged and cookie was refreshed
        customerCookiePortal.navigateTo();
        assertCurrentUrlEquals(customerCookiePortal);
        String currentCookie = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
        assertNotEquals(currentCookie, tokenCookie);
        assertNotEquals(currentCookie, changedTokenCookie);
        
        // logout
        logoutFromCustomerCookiePortal();
    }
    
    // login to customer-cookie-portal and return the KEYCLOAK_ADAPTER_STATE cookie established on adapter
    private String loginToCustomerCookiePortal() {
        customerCookiePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerCookiePortal);
        assertLogged();
        
        // Assert no JSESSIONID cookie
        Assert.assertNull(driver.manage().getCookieNamed("JSESSIONID"));
        
        return driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
    }
    
    private void logoutFromCustomerCookiePortal() {
        String logout = customerCookiePortal.logoutURL();
        driver.navigate().to(logout);
        WaitUtils.waitUntilElement(By.id("customer_portal_logout")).is().present();
        assertNull(driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE));
        customerCookiePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }
    
    protected void assertLogged() {
        assertPageContains("Bill Burke");
        assertPageContains("Stian Thorgersen");
    }
    
    private void assertPageContains(String string) {
        String pageSource = driver.getPageSource();
        assertThat(pageSource, containsString(string));
    }
    
    @Test
    public void testSavedPostRequest() throws InterruptedException {
        // test login to customer-portal which does a bearer request to customer-db
        inputPortal.navigateTo();
        assertCurrentUrlEquals(inputPortal);
        inputPortal.execute("hello");

        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(inputPortal.getUriBuilder().clone().path("secured").path("post").build());
        waitForPageToLoad();
        assertPageContains("parameter=hello");

        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, customerPortal.toString())
                .build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        // test unsecured POST KEYCLOAK-901
        Client client = ClientBuilder.newClient();
        Form form = new Form();
        form.param("parameter", "hello");
        String text = client.target(inputPortal + "/unsecured").request().post(Entity.form(form), String.class);
        assertThat(text, containsString("parameter=hello"));
        client.close();
    }

    @Test
    public void testLoginSSOAndLogout() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        assertTrue(testRealmLoginPage.form().isUsernamePresent());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        assertLogged();

        // test SSO
        productPortal.navigateTo();
        assertCurrentUrlEquals(productPortal);
        assertPageContains("iPhone");
        assertPageContains("iPad");

        // View stats
        List<Map<String, String>> stats = testRealmResource().getClientSessionStats();
        Map<String, String> customerPortalStats = null;
        Map<String, String> productPortalStats = null;
        for (Map<String, String> s : stats) {
            switch (s.get("clientId")) {
                case "customer-portal":
                    customerPortalStats = s;
                    break;
                case "product-portal":
                    productPortalStats = s;
                    break;
            }
        }
        assertEquals(1, Integer.parseInt(customerPortalStats.get("active")));
        assertEquals(1, Integer.parseInt(productPortalStats.get("active")));

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, customerPortal.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Test
    public void testServletRequestLogout() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        assertLogged();

        // test SSO
        productPortal.navigateTo();
        assertCurrentUrlEquals(productPortal);
        assertPageContains("iPhone");
        assertPageContains("iPad");

        // back
        customerPortal.navigateTo();
        assertCurrentUrlEquals(customerPortal);
        assertLogged();
        // test logout

        driver.navigate().to(customerPortal + "/logout");
        WaitUtils.waitUntilElement(By.id("customer_portal_logout")).is().present();
        WaitUtils.waitUntilElement(By.id("customer_database_logout")).is().present();

        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Test
    public void testLoginSSOIdle() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        assertTrue(testRealmLoginPage.form().isUsernamePresent());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        assertLogged();

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalIdle = demoRealmRep.getSsoSessionIdleTimeout();
        try {
            demoRealmRep.setSsoSessionIdleTimeout(1);
            testRealmResource().update(demoRealmRep);

            // Needs to add some additional time due the tolerance allowed by IDLE_TIMEOUT_WINDOW_SECONDS
            setAdapterAndServerTimeOffset(2 + SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS);

            productPortal.navigateTo();
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        } finally {
            demoRealmRep.setSsoSessionIdleTimeout(originalIdle);
            testRealmResource().update(demoRealmRep);
        }
    }

    @Test
    public void testLoginSSOIdleRemoveExpiredUserSessions() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        log.info("Current url: " + driver.getCurrentUrl());
        assertTrue(testRealmLoginPage.form().isUsernamePresent());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        log.info("Current url: " + driver.getCurrentUrl());
        assertCurrentUrlEquals(customerPortal);

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalIdle = demoRealmRep.getSsoSessionIdleTimeout();
        try {
            demoRealmRep.setSsoSessionIdleTimeout(1);
            testRealmResource().update(demoRealmRep);

            // Needs to add some additional time due the tolerance allowed by IDLE_TIMEOUT_WINDOW_SECONDS
            setAdapterAndServerTimeOffset(2 + SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS);

            productPortal.navigateTo();
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        } finally {
            // need to cleanup so other tests don't fail, so invalidate http sessions on remote clients.
            demoRealmRep.setSsoSessionIdleTimeout(originalIdle);
            testRealmResource().update(demoRealmRep);
            // note: sessions invalidated after each test, see: AbstractKeycloakTest.afterAbstractKeycloakTest()
        }
    }

    @Test
    public void testLoginSSOMax() throws InterruptedException {
        // Delete cookies
        driver.navigate().to(customerPortal.getUriBuilder().clone().path("error.html").build().toASCIIString());
        driver.manage().deleteAllCookies();

        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        assertTrue(testRealmLoginPage.form().isUsernamePresent());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalMax = demoRealmRep.getSsoSessionMaxLifespan();
        try {
            demoRealmRep.setSsoSessionMaxLifespan(1);
            testRealmResource().update(demoRealmRep);

            TimeUnit.SECONDS.sleep(2);
            productPortal.navigateTo();
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        } finally {
            demoRealmRep.setSsoSessionMaxLifespan(originalMax);
            testRealmResource().update(demoRealmRep);
        }
    }

    //KEYCLOAK-518
    @Test
    public void testNullBearerToken() {
        Client client = new ResteasyClientBuilder().httpEngine(new FollowRedirectsEngine()).build();
        WebTarget target = client.target(customerDb.toString());
        Response response = target.request().get();
        assertEquals(401, response.getStatus());
        response.close();
        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        assertEquals(401, response.getStatus());
        response.close();
        client.close();
    }

    //KEYCLOAK-1368
    @Test
    public void testNullBearerTokenCustomErrorPage() {
        Client client = new ResteasyClientBuilder().httpEngine(new FollowRedirectsEngine()).build();
        WebTarget target = client.target(customerDbErrorPage.toString());

        Response response = target.request().get();

        assertEquals(401, response.getStatus());
        String errorPageResponse = response.readEntity(String.class);
        assertThat(errorPageResponse, containsString("Error Page"));
        assertThat(errorPageResponse, containsString(OIDCAuthenticationError.Reason.NO_BEARER_TOKEN.toString()));
        response.close();

        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();

        assertEquals(401, response.getStatus());
        errorPageResponse = response.readEntity(String.class);
        assertThat(errorPageResponse, containsString("Error Page"));
        assertThat(errorPageResponse, containsString(OIDCAuthenticationError.Reason.INVALID_TOKEN.toString()));
        response.close();
        
        client.close();
    }

    //KEYCLOAK-518
    @Test
    public void testBadUser() {
        Client client = ClientBuilder.newClient();
        URI uri = OIDCLoginProtocolService.tokenUrl(authServerPage.createUriBuilder()).build("demo");
        WebTarget target = client.target(uri);
        String header = BasicAuthHelper.createHeader("customer-portal", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "monkey@redhat.com")
                .param("password", "password");
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
        assertEquals(401, response.getStatus());
        response.close();
        client.close();
    }

    @Test
    public void testAuthenticated() {
        // test login to customer-portal which does a bearer request to customer-db
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(securePortal);
        assertLogged();
        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, securePortal.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Test
    public void testAuthenticatedWithCustomSessionConfig() {
        // test login to customer-portal which does a bearer request to customer-db
        securePortalWithCustomSessionConfig.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(securePortalWithCustomSessionConfig);

        assertThat("Cookie CUSTOM_JSESSION_ID_NAME should exist", driver.manage().getCookieNamed("CUSTOM_JSESSION_ID_NAME"), notNullValue());

        assertLogged();
        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, securePortalWithCustomSessionConfig.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        securePortalWithCustomSessionConfig.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Test
    public void testRewriteRedirectUri() {
        // test login to application where the redirect_uri is rewritten based on rules defined in keycloak.json
        securePortalRewriteRedirectUri.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertTrue(driver.getCurrentUrl().contains("/rewritten"));
    }

    // Tests "token-minimum-time-to-live" adapter configuration option
    @Test
    public void testTokenMinTTL() {
        // Login
        tokenMinTTLPage.navigateTo();
        assertTrue(testRealmLoginPage.form().isUsernamePresent());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(tokenMinTTLPage);

        // Get time of token
        AccessToken token = tokenMinTTLPage.getAccessToken();
        int tokenIssued1 = token.getIssuedAt();

        // Sets 5 minutes offset and assert access token will be still the same
        setAdapterAndServerTimeOffset(300, tokenMinTTLPage.toString());
        tokenMinTTLPage.navigateTo();
        token = tokenMinTTLPage.getAccessToken();
        int tokenIssued2 = token.getIssuedAt();
        Assert.assertEquals(tokenIssued1, tokenIssued2);
        assertFalse(token.isExpired());

        // Sets 9 minutes offset and assert access token will be refreshed (accessTokenTimeout is 10 minutes, token-min-ttl is 2 minutes. Hence 8 minutes or more should be sufficient)
        setAdapterAndServerTimeOffset(540, tokenMinTTLPage.toString());
        tokenMinTTLPage.navigateTo();
        token = tokenMinTTLPage.getAccessToken();
        int tokenIssued3 = token.getIssuedAt();
        Assert.assertTrue(tokenIssued3 > tokenIssued1);

        // Revert times
        setAdapterAndServerTimeOffset(0, tokenMinTTLPage.toString());
    }

    // Tests forwarding of parameters like "prompt"
    @Test
    public void testOIDCParamsForwarding() {
        // test login to customer-portal which does a bearer request to customer-db
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        waitForPageToLoad();
        assertCurrentUrlStartsWith(securePortal);
        assertLogged();

        int currentTime = Time.currentTime();
        try {
            setAdapterAndServerTimeOffset(10, securePortal.toString());

            // Test I need to reauthenticate with prompt=login
            String appUri = tokenMinTTLPage.getUriBuilder().queryParam(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_LOGIN).build().toString();
            URLUtils.navigateToUri(appUri);
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
            testRealmLoginPage.form().login("bburke@redhat.com", "password");
            AccessToken token = tokenMinTTLPage.getAccessToken();
            int authTime = token.getAuthTime();
            assertThat(authTime, is(greaterThanOrEqualTo(currentTime + 10)));
        } finally {
            setAdapterAndServerTimeOffset(0, securePortal.toString());
        }
    }

    private static Map<String, String> getQueryFromUrl(String url) {
        try {
            return URLEncodedUtils.parse(new URI(url), "UTF-8").stream()
                .collect(Collectors.toMap(p -> p.getName(), p -> p.getValue()));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Test
    public void testOIDCUiLocalesParamForwarding() {
        ProfileAssume.assumeCommunity();

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        boolean enabled = demoRealmRep.isInternationalizationEnabled();
        String defaultLocale = demoRealmRep.getDefaultLocale();
        Set<String> locales = demoRealmRep.getSupportedLocales();
        demoRealmRep.setInternationalizationEnabled(true);
        demoRealmRep.setDefaultLocale("en");
        demoRealmRep.setSupportedLocales(Stream.of("en", "de").collect(Collectors.toSet()));
        testRealmResource().update(demoRealmRep);

        try {
            // test login with ui_locales to de+en
            String portalUri = securePortal.getUriBuilder().build().toString();
            UriBuilder uriBuilder = securePortal.getUriBuilder();
            String appUri = uriBuilder.clone().queryParam(OAuth2Constants.UI_LOCALES_PARAM, "de en").build().toString();
            URLUtils.navigateToUri(appUri);
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
            // check the ui_locales param is there
            Map<String, String> parameters = getQueryFromUrl(driver.getCurrentUrl());
            assertThat(parameters.get(OAuth2Constants.UI_LOCALES_PARAM), allOf(containsString("de"), containsString("en")));

            String appUriDe = uriBuilder.clone().queryParam(OAuth2Constants.UI_LOCALES_PARAM, "de").build().toString();
            URLUtils.navigateToUri(appUriDe);
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

            // check that the page is in german
            assertPageContains("Passwort");
            testRealmLoginPage.form().login("bburke@redhat.com", "password");
            // check no ui_locales in the final url adapter url
            assertCurrentUrlEquals(portalUri);
            assertLogged();
            // logout
            String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                    .queryParam(OAuth2Constants.REDIRECT_URI, securePortal.toString()).build("demo").toString();
            driver.navigate().to(logoutUri);
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
            securePortal.navigateTo();
            assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        } finally {
            demoRealmRep.setInternationalizationEnabled(enabled);
            demoRealmRep.setDefaultLocale(defaultLocale);
            demoRealmRep.setSupportedLocales(locales);
            testRealmResource().update(demoRealmRep);
        }
    }


    @Test
    public void testVerifyTokenAudience() throws Exception {
        // Generate audience client scope
        String clientScopeId = testingClient.testing().generateAudienceClientScope("demo", "customer-db-audience-required");

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("demo"), "customer-portal");
        client.addOptionalClientScope(clientScopeId);

        // Login without audience scope. Invoke service should end with failure
        driver.navigate().to(customerPortal.callCustomerDbAudienceRequiredUrl(false).toURL());
        assertTrue(testRealmLoginPage.form().isUsernamePresent());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal.callCustomerDbAudienceRequiredUrl(false));

        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Service returned: 401"));
        Assert.assertFalse(pageSource.contains("Stian Thorgersen"));

        // Logout TODO: will be good to not request logout to force adapter to use additional scope (and other request parameters)
        driver.navigate().to(customerPortal.logout().toURL());
        waitForPageToLoad();

        // Login with requested audience
        driver.navigate().to(customerPortal.callCustomerDbAudienceRequiredUrl(true).toURL());
        assertTrue(testRealmLoginPage.form().isUsernamePresent());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal.callCustomerDbAudienceRequiredUrl(false));

        pageSource = driver.getPageSource();
        Assert.assertFalse(pageSource.contains("Service returned: 401"));
        assertLogged();

        // logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, customerPortal.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }


    @Test
    public void testBasicAuth() {
        String value = "hello";
        Client client = ClientBuilder.newClient();

        //pause(1000000);

        Response response = client.target(basicAuthPage
                .setTemplateValues(value).buildUri()).request().header("Authorization", BasicAuthHelper.createHeader("mposolda", "password")).get();

        assertThat(response, Matchers.statusCodeIs(Status.OK));
        assertEquals(value, response.readEntity(String.class));
        response.close();

        response = client.target(basicAuthPage
                .setTemplateValues(value).buildUri()).request().header("Authorization", BasicAuthHelper.createHeader("invalid-user", "password")).get();
        assertThat(response, Matchers.statusCodeIs(Status.UNAUTHORIZED));
        assertThat(response, Matchers.body(anyOf(containsString("Unauthorized"), containsString("Status 401"))));

        response = client.target(basicAuthPage
                .setTemplateValues(value).buildUri()).request().header("Authorization", BasicAuthHelper.createHeader("admin", "invalid-password")).get();
        assertThat(response, Matchers.statusCodeIs(Status.UNAUTHORIZED));
        assertThat(response, Matchers.body(anyOf(containsString("Unauthorized"), containsString("Status 401"))));

        client.close();
    }

    @Test
    public void grantServerBasedApp() {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "customer-portal");
        ClientRepresentation client = clientResource.toRepresentation();
        client.setConsentRequired(true);
        clientResource.update(client);

        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("REVOKE_GRANT", "LOGIN"));
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));
        testRealmResource().update(realm);

        customerPortal.navigateTo();

        loginPage.form().login("bburke@redhat.com", "password");

        assertTrue(oAuthGrantPage.isCurrent());

        oAuthGrantPage.accept();

        waitForPageToLoad();
        assertLogged();

        String userId = ApiUtil.findUserByUsername(testRealmResource(), "bburke@redhat.com").getId();

        assertEvents.expectLogin()
                .realm(realm.getId())
                .client("customer-portal")
                .user(userId)
                .detail(Details.USERNAME, "bburke@redhat.com")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .detail(Details.REDIRECT_URI,
                        org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.equalTo(customerPortal.getInjectedUrl().toString()),
                                org.hamcrest.Matchers.equalTo(customerPortal.getInjectedUrl().toString() + "/")))
                .removeDetail(Details.CODE_ID)
                .assertEvent();

        assertEvents.expectCodeToToken(null, null)
                .realm(realm.getId())
                .client("customer-portal")
                .user(userId)
                .session(AssertEvents.isUUID())
                .removeDetail(Details.CODE_ID)
                .assertEvent();

        applicationsPage.navigateTo();
        applicationsPage.revokeGrantForApplication("customer-portal");

        customerPortal.navigateTo();

        assertTrue(oAuthGrantPage.isCurrent());

        assertEvents.expect(EventType.REVOKE_GRANT)
                .realm(realm.getId())
                .client("account")
                .user(userId)
                .detail(Details.REVOKED_CLIENT, "customer-portal")
                .assertEvent();

        assertEvents.assertEmpty();

        // Revert consent
        client = clientResource.toRepresentation();
        client.setConsentRequired(false);
        clientResource.update(client);
    }

    @Test
    public void historyOfAccessResourceTest() throws IOException {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("LOGIN", "LOGIN_ERROR", "LOGOUT", "CODE_TO_TOKEN"));
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));
        testRealmResource().update(realm);

        customerPortal.navigateTo();

        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        waitForPageToLoad();
        assertLogged();

        String userId = ApiUtil.findUserByUsername(testRealmResource(), "bburke@redhat.com").getId();

        assertEvents.expectLogin()
                .realm(realm.getId())
                .client("customer-portal")
                .user(userId)
                .detail(Details.USERNAME, "bburke@redhat.com")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED)
                .detail(Details.REDIRECT_URI,
                        org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.equalTo(customerPortal.getInjectedUrl().toString()),
                                org.hamcrest.Matchers.equalTo(customerPortal.getInjectedUrl().toString() + "/")))
                .removeDetail(Details.CODE_ID)
                .assertEvent();

        assertEvents.expectCodeToToken(null, null)
                .realm(realm.getId())
                .client("customer-portal")
                .user(userId)
                .session(AssertEvents.isUUID())
                .removeDetail(Details.CODE_ID)
                .assertEvent();


        driver.navigate().to(testRealmPage.getOIDCLogoutUrl() + "?redirect_uri=" + customerPortal);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        assertEvents.expectLogout(null)
                .realm(realm.getId())
                .user(userId)
                .session(AssertEvents.isUUID())
                .detail(Details.REDIRECT_URI,
                        org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.equalTo(customerPortal.getInjectedUrl().toString()),
                                org.hamcrest.Matchers.equalTo(customerPortal.getInjectedUrl().toString() + "/")))
                .assertEvent();

        assertEvents.assertEmpty();

        String serverLogPath = null;

        String appServer = System.getProperty("app.server");
        if (appServer != null && (appServer.equals("wildfly") || appServer.equals("eap6") || appServer.equals("eap"))) {
            serverLogPath = System.getProperty("app.server.home") + "/standalone-test/log/server.log";
        }

        String appServerUrl;
        if (Boolean.parseBoolean(System.getProperty("app.server.ssl.required"))) {
            appServerUrl = "https://localhost:" + System.getProperty("app.server.https.port", "8543") + "/";
        } else {
            appServerUrl = "http://localhost:" + System.getProperty("app.server.http.port", "8280") + "/";
        }

        if (serverLogPath != null) {
            log.info("Checking app server log at: " + serverLogPath);
            File serverLog = new File(serverLogPath);
            String serverLogContent = FileUtils.readFileToString(serverLog, "UTF-8");
            UserRepresentation bburke = ApiUtil.findUserByUsername(testRealmResource(), "bburke@redhat.com");

            //the expected log message has DEBUG level
            assertThat(serverLogContent, containsString("User '" + bburke.getId() + "' invoking '" + appServerUrl + "customer-db/' on client 'customer-db'"));
        } else {
            log.info("Checking app server log on app-server: \"" + System.getProperty("app.server") + "\" is not supported.");
        }
    }

    @Test
    public void testWithoutKeycloakConf() {
        customerPortalNoConf.navigateTo();
        String pageSource = driver.getPageSource();
        assertThat(pageSource, anyOf(containsString("Forbidden"), containsString("forbidden"), containsString("HTTP Status 401")));
    }
    
    // KEYCLOAK-3509
    @Test
    public void testLoginEncodedRedirectUri() {
        // test login to customer-portal which does a bearer request to customer-db
        driver.navigate().to(productPortal.getInjectedUrl() + "?encodeTest=a%3Cb");
        System.out.println("Current url: " + driver.getCurrentUrl());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());
        
        assertCurrentUrlEquals(productPortal + "?encodeTest=a%3Cb");
        assertPageContains("iPhone");
        assertPageContains("uriEncodeTest=true");

        driver.navigate().to(productPortal.getInjectedUrl());
        assertCurrentUrlEquals(productPortal);
        System.out.println(driver.getCurrentUrl());
        assertPageContains("uriEncodeTest=false");
        
        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, customerPortal.toString())
                .build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        
    }
    
    @Test
    public void testAutodetectBearerOnly() {
        Client client = ClientBuilder.newClient();
        
        // Do not redirect client to login page if it's an XHR
        System.out.println(productPortalAutodetectBearerOnly.getInjectedUrl().toString());
        WebTarget target = client.target(productPortalAutodetectBearerOnly.getInjectedUrl().toString() + "/");
        Response response = target.request().header("X-Requested-With", "XMLHttpRequest").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();
        
        // Do not redirect client to login page if it's a partial Faces request
        response = target.request().header("Faces-Request", "partial/ajax").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();
        
        // Do not redirect client to login page if it's a SOAP request
        response = target.request().header("SOAPAction", "").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // Do not redirect client to login page if Accept header is missing
        response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // Do not redirect client to login page if client does not understand HTML reponses
        response = target.request().header(HttpHeaders.ACCEPT, "application/json,text/xml").get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        // Redirect client to login page if it's not an XHR
        response = target.request().header("X-Requested-With", "Dont-Know").header(HttpHeaders.ACCEPT, "*/*").get();
        Assert.assertEquals(302, response.getStatus());
        Assert.assertThat(response.getHeaderString(HttpHeaders.LOCATION), containsString("response_type=code"));
        response.close();

        // Redirect client to login page if client explicitely understands HTML responses
        response = target.request().header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9").get();
        Assert.assertEquals(302, response.getStatus());
        Assert.assertThat(response.getHeaderString(HttpHeaders.LOCATION), containsString("response_type=code"));
        response.close();

        // Redirect client to login page if client understands all response types
        response = target.request().header(HttpHeaders.ACCEPT, "*/*").get();
        Assert.assertEquals(302, response.getStatus());
        Assert.assertThat(response.getHeaderString(HttpHeaders.LOCATION), containsString("response_type=code"));
        response.close();
        client.close();
    }

    // KEYCLOAK-3016
    @Test
    public void testBasicAuthErrorHandling() {
        int numberOfConnections = 10;
        Client client = new ResteasyClientBuilder().connectionPoolSize(numberOfConnections).httpEngine(new FollowRedirectsEngine()).build();
        WebTarget target = client.target(customerDb.getInjectedUrl().toString());
        Response response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        final int LIMIT = numberOfConnections + 1;
        for (int i = 0; i < LIMIT; i++) {
            System.out.println("Testing Basic Auth with bad credentials " + i);
            response = target.request().header(HttpHeaders.AUTHORIZATION, "Basic dXNlcm5hbWU6cGFzc3dvcmQ=").get();
            Assert.assertEquals(401, response.getStatus());
            response.close();
        }

        client.close();
    }
    
    // KEYCLOAK-1733
    @Test
    public void testNullQueryParameterAccessToken() {
        Client client = new ResteasyClientBuilder().httpEngine(new FollowRedirectsEngine()).build();
        
        WebTarget target = client.target(customerDb.getInjectedUrl().toString());
        Response response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        target = client.target(customerDb.getInjectedUrl().toString() + "?access_token=");
        response = target.request().get();
        Assert.assertEquals(401, response.getStatus());
        response.close();

        client.close();
    }
    
    // KEYCLOAK-1733
    @Test
    public void testRestCallWithAccessTokenAsQueryParameter() {

        Client client = new ResteasyClientBuilder().httpEngine(new FollowRedirectsEngine()).build();
        try {
            WebTarget webTarget = client.target(testRealmPage.toString() + "/protocol/openid-connect/token");

            Form form = new Form();
            form.param("grant_type", "password");
            form.param("client_id", "customer-portal-public");
            form.param("username", "bburke@redhat.com");
            form.param("password", "password");
            Response response = webTarget.request().post(Entity.form(form));

            Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String accessToken = tokenResponse.getToken();

            // test without token
            response = client.target(customerDb.getInjectedUrl().toString()).request().get();
            Assert.assertEquals(401, response.getStatus());
            response.close();
            // test with access_token as QueryParamter
            response = client.target(customerDb.getInjectedUrl().toString()).queryParam("access_token", accessToken).request().get();
            Assert.assertEquals(200, response.getStatus());
            response.close();
        } finally {
            client.close();
        }
    }
    
    //KEYCLOAK-4765
    @Test
    public void testCallURLWithAccessToken() throws Exception {
        // test login to customer-portal which does a bearer request to customer-db
        URI applicationURL = inputPortalNoAccessToken.getUriBuilder().clone()
                .queryParam("access_token", "invalid_token")
                .build();

        driver.navigate().to(applicationURL.toURL());

        assertEquals(applicationURL.toASCIIString(), driver.getCurrentUrl());
        inputPortalNoAccessToken.execute("hello");
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Test
    public void testClientAuthenticatedInClientSecretJwt() {
        // test login to customer-portal which does a bearer request to customer-db
        // JWS Client Assertion in client_secret_jwt
        // http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
        String targetClientId = "client-secret-jwt-secure-portal";

        expectResultOfClientAuthenticatedInClientSecretJwt(targetClientId, clientSecretJwtSecurePortal);

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, clientSecretJwtSecurePortal.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
    }

    @Test
    public void testClientNotAuthenticatedInClientSecretJwtBySharedSecretOutOfSync() {
        // JWS Client Assertion in client_secret_jwt
        // http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
        String targetClientId = "client-secret-jwt-secure-portal";
        String expectedErrorString = "invalid_client_credentials";

        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), targetClientId);
        ClientRepresentation client = clientResource.toRepresentation();
        client.setSecret("passwordChanged");
        clientResource.update(client);

        expectResultOfClientNotAuthenticatedInClientSecretJwt(targetClientId, expectedErrorString);
    }

    @Test
    public void testClientNotAuthenticatedInClientSecretJwtByAuthnMethodOutOfSync() {
        // JWS Client Assertion in client_secret_jwt
        // http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
        String targetClientId = "client-secret-jwt-secure-portal";
        String expectedErrorString = "invalid_client_credentials";

        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), targetClientId);
        ClientRepresentation client = clientResource.toRepresentation();
        client.setClientAuthenticatorType("client-secret");
        clientResource.update(client);

        expectResultOfClientNotAuthenticatedInClientSecretJwt(targetClientId, expectedErrorString);
    }

    @Test
    public void testClientAuthenticatedInClientSecretJwtValidAlg() {
        String targetClientId = "client-secret-jwt-secure-portal-valid-alg";

        expectResultOfClientAuthenticatedInClientSecretJwt(targetClientId, clientSecretJwtSecurePortalValidAlg);

        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, clientSecretJwtSecurePortalValidAlg.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
    }

    @Test
    public void testTokenInCookieSSORoot() {
        // Login
        String tokenCookie = loginToCustomerCookiePortalRoot();
        Cookie cookie = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE);
        assertEquals("/", cookie.getPath());

        // SSO to second app
        customerPortal.navigateTo();
        assertLogged();

        customerCookiePortalRoot.navigateTo();
        assertLogged();
        cookie = driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE);
        String tokenCookie2 = cookie.getValue();
        assertEquals(tokenCookie, tokenCookie2);
        assertEquals("/", cookie.getPath());

        // Logout with httpServletRequest
        logoutFromCustomerCookiePortalRoot();

        // Also should be logged-out from the second app
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    private String loginToCustomerCookiePortalRoot() {
        customerCookiePortalRoot.navigateTo("relative");
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerCookiePortalRoot.getInjectedUrl().toString() + "relative");
        assertLogged();

        // Assert no JSESSIONID cookie
        Assert.assertNull(driver.manage().getCookieNamed("JSESSIONID"));

        return driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE).getValue();
    }

    private void logoutFromCustomerCookiePortalRoot() {
        String logout = customerCookiePortalRoot.logoutURL();
        driver.navigate().to(logout);
        WaitUtils.waitUntilElement(By.id("customer_portal_logout")).is().present();
        assertNull(driver.manage().getCookieNamed(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE));
        customerCookiePortalRoot.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    private void expectResultOfClientAuthenticatedInClientSecretJwt(String targetClientId, AbstractPageWithInjectedUrl portal) {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("LOGIN", "CODE_TO_TOKEN"));
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));
        testRealmResource().update(realm); 

        portal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        String userId = ApiUtil.findUserByUsername(testRealmResource(), "bburke@redhat.com").getId();

        assertEvents.expectLogin()
        .realm(realm.getId())
        .client(targetClientId)
        .user(userId)
        .detail(Details.USERNAME, "bburke@redhat.com")
        .detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED)
        .detail(Details.REDIRECT_URI,
                org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.equalTo(portal.getInjectedUrl().toString()),
                        org.hamcrest.Matchers.equalTo(portal.getInjectedUrl().toString() + "/")))
        .removeDetail(Details.CODE_ID)
        .assertEvent();

        assertEvents.expectCodeToToken(null, null)
        .realm(realm.getId())
        .client(targetClientId)
        .user(userId)
        .session(AssertEvents.isUUID())
        .clearDetails()
        .assertEvent();
    }

    private void expectResultOfClientNotAuthenticatedInClientSecretJwt(String targetClientId, String expectedErrorString) {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("LOGIN", "CODE_TO_TOKEN_ERROR"));
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));
        testRealmResource().update(realm);

        clientSecretJwtSecurePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        String userId = ApiUtil.findUserByUsername(testRealmResource(), "bburke@redhat.com").getId();

        assertEvents.expectLogin()
                .realm(realm.getId())
                .client(targetClientId)
                .user(userId)
                .detail(Details.USERNAME, "bburke@redhat.com")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED)
                .detail(Details.REDIRECT_URI,
                        org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.equalTo(clientSecretJwtSecurePortal.getInjectedUrl().toString()),
                                org.hamcrest.Matchers.equalTo(clientSecretJwtSecurePortal.getInjectedUrl().toString() + "/")))
                .removeDetail(Details.CODE_ID)
                .assertEvent();

        assertEvents.expectCodeToToken(null, null)
                .realm(realm.getId())
                .client(targetClientId)
                .user((String)null)
                .error(expectedErrorString)
                .clearDetails()
                .assertEvent(); 
    }

    @Test
    public void testLoginHintFromClientRequest() {
        driver.navigate().to(customerPortal + "?login_hint=blah%3d");
        waitForPageToLoad();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        assertThat(testRealmLoginPage.form().getUsername(), is("blah="));
    }
}
