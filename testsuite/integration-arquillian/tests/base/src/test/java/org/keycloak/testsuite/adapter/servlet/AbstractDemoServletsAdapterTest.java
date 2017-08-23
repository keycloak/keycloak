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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Version;
import org.keycloak.common.util.Time;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.VersionRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.*;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.console.page.events.Config;
import org.keycloak.testsuite.console.page.events.LoginEvents;
import org.keycloak.testsuite.util.*;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

import javax.ws.rs.core.Response.Status;

import static org.hamcrest.Matchers.*;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.WaitUtils.*;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractDemoServletsAdapterTest extends AbstractServletsAdapterTest {

    @Page
    private CustomerPortal customerPortal;
    @Page
    private CustomerPortalNoConf customerPortalNoConf;
    @Page
    private SecurePortal securePortal;
    @Page
    private SecurePortalWithCustomSessionConfig securePortalWithCustomSessionConfig;
    @Page
    private CustomerDb customerDb;
    @Page
    private CustomerDbErrorPage customerDbErrorPage;
    @Page
    private ProductPortal productPortal;
    @Page
    private InputPortal inputPortal;
    @Page
    private TokenMinTTLPage tokenMinTTLPage;
    @Page
    private OAuthGrant oAuthGrantPage;
    @Page
    private Applications applicationsPage;
    @Page
    private LoginEvents loginEventsPage;
    @Page
    private BasicAuth basicAuthPage;
    @Page
    private Config configPage;

    @Rule
    public AssertEvents assertEvents = new AssertEvents(this);

    @Deployment(name = CustomerPortal.DEPLOYMENT_NAME)
    protected static WebArchive customerPortal() {
        return servletDeployment(CustomerPortal.DEPLOYMENT_NAME, CustomerServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = CustomerPortalNoConf.DEPLOYMENT_NAME)
    protected static WebArchive customerPortalNoConf() {
        return servletDeployment(CustomerPortalNoConf.DEPLOYMENT_NAME, CustomerServletNoConf.class, ErrorServlet.class);
    }

    @Deployment(name = SecurePortal.DEPLOYMENT_NAME)
    protected static WebArchive securePortal() {
        return servletDeployment(SecurePortal.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }

    @Deployment(name = SecurePortalWithCustomSessionConfig.DEPLOYMENT_NAME)
    protected static WebArchive securePortalWithCustomSessionConfig() {
        return servletDeployment(SecurePortalWithCustomSessionConfig.DEPLOYMENT_NAME, CallAuthenticatedServlet.class);
    }

    @Deployment(name = CustomerDb.DEPLOYMENT_NAME)
    protected static WebArchive customerDb() {
        return servletDeployment(CustomerDb.DEPLOYMENT_NAME, AdapterActionsFilter.class, CustomerDatabaseServlet.class);
    }

    @Deployment(name = CustomerDbErrorPage.DEPLOYMENT_NAME)
    protected static WebArchive customerDbErrorPage() {
        return servletDeployment(CustomerDbErrorPage.DEPLOYMENT_NAME, CustomerDatabaseServlet.class, ErrorServlet.class);
    }

    @Deployment(name = ProductPortal.DEPLOYMENT_NAME)
    protected static WebArchive productPortal() {
        return servletDeployment(ProductPortal.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @Deployment(name = InputPortal.DEPLOYMENT_NAME)
    protected static WebArchive inputPortal() {
        return servletDeployment(InputPortal.DEPLOYMENT_NAME, "keycloak.json", InputServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = TokenMinTTLPage.DEPLOYMENT_NAME)
    protected static WebArchive tokenMinTTLPage() {
        return servletDeployment(TokenMinTTLPage.DEPLOYMENT_NAME, AdapterActionsFilter.class, AbstractShowTokensServlet.class, TokenMinTTLServlet.class, ErrorServlet.class);
    }

    @Deployment(name = BasicAuth.DEPLOYMENT_NAME)
    protected static WebArchive basicAuth() {
        return servletDeployment(BasicAuth.DEPLOYMENT_NAME, BasicAuthServlet.class);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        configPage.setConsoleRealm(DEMO);
        loginEventsPage.setConsoleRealm(DEMO);
        applicationsPage.setAuthRealm(DEMO);
        loginEventsPage.setConsoleRealm(DEMO);
    }

    @Before
    public void beforeDemoServletsAdapterTest() {
        // Delete all cookies from token-min-ttl page to be sure we are logged out
        tokenMinTTLPage.navigateTo();
        driver.manage().deleteAllCookies();
    }

    @Test
    public void testSavedPostRequest() throws InterruptedException {
        // test login to customer-portal which does a bearer request to customer-db
        inputPortal.navigateTo();
        assertCurrentUrlEquals(inputPortal);
        inputPortal.execute("hello");

        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(driver, inputPortal + "/secured/post");
        waitForPageToLoad();
        String pageSource = driver.getPageSource();
        assertThat(pageSource, containsString("parameter=hello"));

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
        assertTrue(text.contains("parameter=hello"));
        client.close();
    }

    @Test
    public void testLoginSSOAndLogout() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        productPortal.navigateTo();
        assertCurrentUrlEquals(productPortal);
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

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
//        testRealmLoginPage.form().cancel();
//        assertTrue(driver.getPageSource().contains("Error Page"));
    }

    @Test
    public void testServletRequestLogout() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // test SSO
        productPortal.navigateTo();
        assertCurrentUrlEquals(productPortal);
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));

        // back
        customerPortal.navigateTo();
        assertCurrentUrlEquals(customerPortal);
        pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
        // test logout

        driver.navigate().to(customerPortal + "/logout");
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("servlet logout ok"));
        assertTrue(pageSource.contains("servlet logout from database ok"));

        customerPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Test
    public void testLoginSSOIdle() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalIdle = demoRealmRep.getSsoSessionIdleTimeout();
        demoRealmRep.setSsoSessionIdleTimeout(1);
        testRealmResource().update(demoRealmRep);

        pause(2000);

        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        demoRealmRep.setSsoSessionIdleTimeout(originalIdle);
        testRealmResource().update(demoRealmRep);
    }

    @Test
    public void testLoginSSOIdleRemoveExpiredUserSessions() {
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        log.info("Current url: " + driver.getCurrentUrl());
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        log.info("Current url: " + driver.getCurrentUrl());
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        log.info(pageSource);
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalIdle = demoRealmRep.getSsoSessionIdleTimeout();
        demoRealmRep.setSsoSessionIdleTimeout(1);
        testRealmResource().update(demoRealmRep);

        pause(2000);

        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        // need to cleanup so other tests don't fail, so invalidate http sessions on remote clients.
        demoRealmRep.setSsoSessionIdleTimeout(originalIdle);
        testRealmResource().update(demoRealmRep);
        // note: sessions invalidated after each test, see: AbstractKeycloakTest.afterAbstractKeycloakTest()

    }

    @Test
    public void testLoginSSOMax() throws InterruptedException {
        // Delete cookies
        driver.navigate().to(customerPortal + "/error.html");
        driver.manage().deleteAllCookies();

        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalMax = demoRealmRep.getSsoSessionMaxLifespan();
        demoRealmRep.setSsoSessionMaxLifespan(1);
        testRealmResource().update(demoRealmRep);

        TimeUnit.SECONDS.sleep(2);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        demoRealmRep.setSsoSessionMaxLifespan(originalMax);
        testRealmResource().update(demoRealmRep);

        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, securePortal.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
    }

    @Test
    public void testNullBearerToken() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(customerDb.toString());
        Response response = target.request().get();
        assertEquals(401, response.getStatus());
        response.close();
        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        assertEquals(401, response.getStatus());
        response.close();
        client.close();
    }

    @Test
    @Ignore
    public void testNullBearerTokenCustomErrorPage() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(customerDbErrorPage.toString());
        Response response = target.request().get();

        // TODO: follow redirects automatically if possible
        if (response.getStatus() == 302) {
            String location = response.getHeaderString(HttpHeaders.LOCATION);
            response.close();
            response = client.target(location).request().get();
        }
        assertEquals(200, response.getStatus());
        String errorPageResponse = response.readEntity(String.class);
        assertTrue(errorPageResponse.contains("Error Page"));
        response.close();

        response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer null").get();
        // TODO: follow redirects automatically if possible
        if (response.getStatus() == 302) {
            String location = response.getHeaderString(HttpHeaders.LOCATION);
            response.close();
            response = client.target(location).request().get();
        }
        assertEquals(200, response.getStatus());
        errorPageResponse = response.readEntity(String.class);
        assertTrue(errorPageResponse.contains("Error Page"));
        response.close();

        client.close();
    }

    @Test
    @Ignore
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
    public void testVersion() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(authServerPage.createUriBuilder()).path("version");
        VersionRepresentation version = target.request().get(VersionRepresentation.class);
        assertNotNull(version);
        assertNotNull(version.getVersion());
        assertNotNull(version.getBuildTime());
        assertNotEquals(version.getVersion(), Version.UNKNOWN);
        assertNotEquals(version.getBuildTime(), Version.UNKNOWN);

        VersionRepresentation version2 = client.target(securePortal.toString()).path(AdapterConstants.K_VERSION).request().get(VersionRepresentation.class);
        assertNotNull(version2);
        assertNotNull(version2.getVersion());
        assertNotNull(version2.getBuildTime());
        if (!suiteContext.isAdapterCompatTesting()) {
            assertEquals(version.getVersion(), version2.getVersion());
            assertEquals(version.getBuildTime(), version2.getBuildTime());
        }
        client.close();
    }

    @Test
    public void testAuthenticated() {
        // test login to customer-portal which does a bearer request to customer-db
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(securePortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
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

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));
        // test logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, securePortalWithCustomSessionConfig.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        securePortalWithCustomSessionConfig.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    // Tests "token-minimum-time-to-live" adapter configuration option
    @Test
    public void testTokenMinTTL() {
        // Login
        tokenMinTTLPage.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
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
        assertCurrentUrlEquals(securePortal);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        int currentTime = Time.currentTime();
        setAdapterAndServerTimeOffset(10, securePortal.toString());

        // Test I need to reauthenticate with prompt=login
        String appUri = tokenMinTTLPage.getUriBuilder().queryParam(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_LOGIN).build().toString();
        URLUtils.navigateToUri(appUri, true);
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        AccessToken token = tokenMinTTLPage.getAccessToken();
        int authTime = token.getAuthTime();
        Assert.assertTrue(currentTime + 10 <= authTime);

        // Revert times
        setAdapterAndServerTimeOffset(0, tokenMinTTLPage.toString());
    }

    @Test
    public void testBasicAuth() {
        String value = "hello";
        Client client = ClientBuilder.newClient();

        //pause(1000000);

        Response response = client.target(basicAuthPage
                .setTemplateValues("mposolda", "password", value).buildUri()).request().get();

        assertThat(response, Matchers.statusCodeIs(Status.OK));
        assertEquals(value, response.readEntity(String.class));
        response.close();

        response = client.target(basicAuthPage
                .setTemplateValues("invalid-user", "password", value).buildUri()).request().get();
        assertThat(response, Matchers.statusCodeIs(Status.UNAUTHORIZED));
        assertThat(response, Matchers.body(anyOf(containsString("Unauthorized"), containsString("Status 401"))));

        response = client.target(basicAuthPage
                .setTemplateValues("admin", "invalid-password", value).buildUri()).request().get();
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

        String pageSource = driver.getPageSource();
        waitForPageToLoad();
        assertThat(pageSource, containsString("Bill Burke"));
        assertThat(pageSource, containsString("Stian Thorgersen"));

        String userId = ApiUtil.findUserByUsername(testRealmResource(), "bburke@redhat.com").getId();

        assertEvents.expectLogin()
                .realm(realm.getId())
                .client("customer-portal")
                .user(userId)
                .detail(Details.USERNAME, "bburke@redhat.com")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .detail(Details.REDIRECT_URI, customerPortal.getInjectedUrl().toString())
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
        String pageSource = driver.getPageSource();
        assertThat(pageSource, containsString("Bill Burke"));
        assertThat(pageSource, containsString("Stian Thorgersen"));

        String userId = ApiUtil.findUserByUsername(testRealmResource(), "bburke@redhat.com").getId();

        assertEvents.expectLogin()
                .realm(realm.getId())
                .client("customer-portal")
                .user(userId)
                .detail(Details.USERNAME, "bburke@redhat.com")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED)
                .detail(Details.REDIRECT_URI, customerPortal.getInjectedUrl().toString())
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
                .detail(Details.REDIRECT_URI, customerPortal.getInjectedUrl().toString())
                .assertEvent();

        assertEvents.assertEmpty();

        String serverLogPath = null;

        String appServer = System.getProperty("app.server");
        if (appServer != null && (appServer.equals("wildfly") || appServer.equals("eap6") || appServer.equals("eap"))) {
            serverLogPath = System.getProperty("app.server.home") + "/standalone/log/server.log";
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
            String serverLogContent = FileUtils.readFileToString(serverLog);
            UserRepresentation bburke = ApiUtil.findUserByUsername(testRealmResource(), "bburke@redhat.com");

            Pattern pattern = Pattern.compile("User '" + bburke.getId() + "' invoking '" + appServerUrl + "customer-portal[^\\s]+' on client 'customer-portal'");
            Matcher matcher = pattern.matcher(serverLogContent);

            assertTrue(matcher.find());
            assertTrue(serverLogContent.contains("User '" + bburke.getId() + "' invoking '" + appServerUrl + "customer-db/' on client 'customer-db'"));
        } else {
            log.info("Checking app server log on app-server: \"" + System.getProperty("app.server") + "\" is not supported.");
        }
    }

    @Test
    public void testWithoutKeycloakConf() {
        customerPortalNoConf.navigateTo();
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Forbidden") || pageSource.contains("HTTP Status 401"));
    }

}
