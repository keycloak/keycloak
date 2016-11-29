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
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Version;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.keys.KeyProvider;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.VersionRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.BasicAuth;
import org.keycloak.testsuite.adapter.page.CustomerDb;
import org.keycloak.testsuite.adapter.page.CustomerDbErrorPage;
import org.keycloak.testsuite.adapter.page.CustomerPortal;
import org.keycloak.testsuite.adapter.page.InputPortal;
import org.keycloak.testsuite.adapter.page.ProductPortal;
import org.keycloak.testsuite.adapter.page.SecurePortal;
import org.keycloak.testsuite.adapter.page.TokenMinTTLPage;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.console.page.events.Config;
import org.keycloak.testsuite.console.page.events.LoginEvents;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.keycloak.testsuite.adapter.page.CustomerPortalNoConf;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

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

    @Deployment(name = CustomerDb.DEPLOYMENT_NAME)
    protected static WebArchive customerDb() {
        return servletDeployment(CustomerDb.DEPLOYMENT_NAME, CustomerDatabaseServlet.class);
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
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("parameter=hello"));

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
    public void testRealmKeyRotationWithNewKeyDownload() throws Exception {
        // Login success first
        tokenMinTTLPage.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(tokenMinTTLPage);

        AccessToken token = tokenMinTTLPage.getAccessToken();
        Assert.assertEquals("bburke@redhat.com", token.getPreferredUsername());

        // Logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, tokenMinTTLPage.toString())
                .build("demo").toString();
        driver.navigate().to(logoutUri);

        // Generate new realm key
        String realmId = adminClient.realm(DEMO).toRepresentation().getId();
        ComponentRepresentation keys = new ComponentRepresentation();
        keys.setName("generated");
        keys.setProviderType(KeyProvider.class.getName());
        keys.setProviderId("rsa-generated");
        keys.setParentId(realmId);
        keys.setConfig(new MultivaluedHashMap<>());
        keys.getConfig().putSingle("priority", "100");
        Response response = adminClient.realm(DEMO).components().add(keys);
        assertEquals(201, response.getStatus());
        response.close();

        String adapterActionsUrl = tokenMinTTLPage.toString() + "/unsecured/foo";
        setAdapterAndServerTimeOffset(300, adapterActionsUrl);

        // Try to login. Should work now due to realm key change
        tokenMinTTLPage.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(tokenMinTTLPage);
        token = tokenMinTTLPage.getAccessToken();
        Assert.assertEquals("bburke@redhat.com", token.getPreferredUsername());
        driver.navigate().to(logoutUri);

        // Revert public keys change
        String timeOffsetUri = UriBuilder.fromUri(adapterActionsUrl)
                .queryParam(AdapterActionsFilter.RESET_PUBLIC_KEY_PARAM, "true")
                .build().toString();
        driver.navigate().to(timeOffsetUri);
        waitUntilElement(By.tagName("body")).is().visible();

        setAdapterAndServerTimeOffset(0, adapterActionsUrl);
    }

    @Test
    public void testClientWithJwksUri() throws Exception {
        // Set client to bad JWKS URI
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "secure-portal");
        ClientRepresentation client = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper wrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        wrapper.setUseJwksUrl(true);
        wrapper.setJwksUrl(securePortal + "/bad-jwks-url");
        clientResource.update(client);

        // Login should fail at the code-to-token
        securePortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        String pageSource = driver.getPageSource();
        assertCurrentUrlStartsWith(securePortal);
        assertFalse(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // Set client to correct JWKS URI
        client = clientResource.toRepresentation();
        wrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        wrapper.setUseJwksUrl(true);
        wrapper.setJwksUrl(securePortal + "/" + AdapterConstants.K_JWKS);
        clientResource.update(client);

        // Login to secure-portal should be fine now. Client keys downloaded from JWKS URI
        securePortal.navigateTo();
        assertCurrentUrlEquals(securePortal);
        pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        // Logout
        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, securePortal.toString()).build("demo").toString();
        driver.navigate().to(logoutUri);
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
        // test login to customer-portal which does a bearer request to customer-db
        customerPortal.navigateTo();
        testRealmLoginPage.form().waitForUsernameInputPresent();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(customerPortal);
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Bill Burke") && pageSource.contains("Stian Thorgersen"));

        RealmRepresentation demoRealmRep = testRealmResource().toRepresentation();
        int originalIdle = demoRealmRep.getSsoSessionMaxLifespan();
        demoRealmRep.setSsoSessionMaxLifespan(1);
        testRealmResource().update(demoRealmRep);

        TimeUnit.SECONDS.sleep(2);
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        demoRealmRep.setSsoSessionIdleTimeout(originalIdle);
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
        assertEquals(version.getVersion(), version2.getVersion());
        assertEquals(version.getBuildTime(), version2.getBuildTime());
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
        URLUtils.navigateToUri(driver, appUri, true);
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

        assertEquals(200, response.getStatus());
        assertEquals(value, response.readEntity(String.class));
        response.close();

        response = client.target(basicAuthPage
                .setTemplateValues("invalid-user", "password", value).buildUri()).request().get();
        assertEquals(401, response.getStatus());
        String readResponse = response.readEntity(String.class);
        assertTrue(readResponse.contains("Unauthorized") || readResponse.contains("Status 401"));
        response.close();

        response = client.target(basicAuthPage
                .setTemplateValues("admin", "invalid-password", value).buildUri()).request().get();
        assertEquals(401, response.getStatus());
        readResponse = response.readEntity(String.class);
        assertTrue(readResponse.contains("Unauthorized") || readResponse.contains("Status 401"));
        response.close();

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
        testRealmResource().update(realm);

        customerPortal.navigateTo();

        loginPage.form().login("bburke@redhat.com", "password");

        assertTrue(oAuthGrantPage.isCurrent());

        oAuthGrantPage.accept();

        waitUntilElement(By.xpath("//body")).text().contains("Bill Burke");
        waitUntilElement(By.xpath("//body")).text().contains("Stian Thorgersen");

        applicationsPage.navigateTo();
        applicationsPage.revokeGrantForApplication("customer-portal");

        customerPortal.navigateTo();

        assertTrue(oAuthGrantPage.isCurrent());

        loginEventsPage.navigateTo();

        if (!testContext.isAdminLoggedIn()) {
            loginPage.form().login(adminUser);
            testContext.setAdminLoggedIn(true);
        }

        loginEventsPage.table().filter();
        loginEventsPage.table().filterForm().addEventType("REVOKE_GRANT");
        loginEventsPage.table().update();

        List<WebElement> resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='REVOKE_GRANT']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='account']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='revoked_client']/../td[text()='customer-portal']"));

        loginEventsPage.table().reset();
        loginEventsPage.table().filterForm().addEventType("LOGIN");
        loginEventsPage.table().update();
        resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='LOGIN']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='customer-portal']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='username']/../td[text()='bburke@redhat.com']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='consent']/../td[text()='consent_granted']"));

        configPage.navigateTo();
        configPage.form().clearLoginEvents();
        driver.findElement(By.xpath("//div[@class='modal-dialog']//button[text()='Delete']")).click();
    }

    @Test
    public void historyOfAccessResourceTest() throws IOException {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("LOGIN", "LOGIN_ERROR", "LOGOUT", "CODE_TO_TOKEN"));
        testRealmResource().update(realm);

        customerPortal.navigateTo();

        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        waitUntilElement(By.xpath("//body")).text().contains("Bill Burke");
        waitUntilElement(By.xpath("//body")).text().contains("Stian Thorgersen");

        driver.navigate().to(testRealmPage.getOIDCLogoutUrl() + "?redirect_uri=" + customerPortal);

        loginEventsPage.navigateTo();

        if (!testContext.isAdminLoggedIn()) {
            loginPage.form().login(adminUser);
            testContext.setAdminLoggedIn(true);
        }

        loginEventsPage.table().filter();
        loginEventsPage.table().filterForm().addEventType("LOGOUT");
        loginEventsPage.table().update();

        List<WebElement> resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='LOGOUT']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));

        loginEventsPage.table().reset();
        loginEventsPage.table().filterForm().addEventType("LOGIN");
        loginEventsPage.table().update();
        resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='LOGIN']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='customer-portal']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='username']/../td[text()='bburke@redhat.com']"));

        loginEventsPage.table().reset();
        loginEventsPage.table().filterForm().addEventType("CODE_TO_TOKEN");
        loginEventsPage.table().update();
        resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath(".//td[text()='CODE_TO_TOKEN']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='customer-portal']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='refresh_token_type']/../td[text()='Refresh']"));

        configPage.navigateTo();
        configPage.form().clearLoginEvents();
        driver.findElement(By.xpath("//div[@class='modal-dialog']//button[text()='Delete']")).click();

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
