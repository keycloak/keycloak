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

package org.keycloak.testsuite.adapter.example;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.JSConsoleTestApp;
import org.keycloak.testsuite.adapter.page.JSDatabaseTestApp;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.console.page.events.Config;
import org.keycloak.testsuite.console.page.events.LoginEvents;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.keycloak.testsuite.auth.page.AuthRealm.EXAMPLE;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

public abstract class AbstractJSConsoleExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private JSConsoleTestApp jsConsoleTestAppPage;

    @Page
    private Config configPage;

    @Page
    private LoginEvents loginEventsPage;

    @Page
    private OAuthGrant oAuthGrantPage;

    @Page
    private Applications applicationsPage;

    private static int TIME_SKEW_TOLERANCE = 3;

    public static int TOKEN_LIFESPAN_LEEWAY = 3; // seconds

    @Deployment(name = JSConsoleTestApp.DEPLOYMENT_NAME)
    private static WebArchive jsConsoleTestApp() throws IOException {
        return exampleDeployment(JSConsoleTestApp.CLIENT_ID);
    }

    @Deployment(name = JSDatabaseTestApp.DEPLOYMENT_NAME)
    private static WebArchive jsDbApp() throws IOException {
        return exampleDeployment(JSDatabaseTestApp.CLIENT_ID);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation jsConsoleRealm = loadRealm(new File(TEST_APPS_HOME_DIR + "/js-console/example-realm.json"));

        fixClientUrisUsingDeploymentUrl(jsConsoleRealm,
                JSConsoleTestApp.CLIENT_ID, jsConsoleTestAppPage.buildUri().toASCIIString());

        jsConsoleRealm.setAccessTokenLifespan(30 + TOKEN_LIFESPAN_LEEWAY); // seconds

        testRealms.add(jsConsoleRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(EXAMPLE);
    }

    @Test
    public void testJSConsoleAuth() {
        jsConsoleTestAppPage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);

        waitUntilElement(jsConsoleTestAppPage.getInitButtonElement()).is().present();

        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.logIn();
        testRealmLoginPage.form().login("user", "invalid-password");
        assertCurrentUrlDoesntStartWith(jsConsoleTestAppPage);

        testRealmLoginPage.form().login("invalid-user", "password");
        assertCurrentUrlDoesntStartWith(jsConsoleTestAppPage);

        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);
        jsConsoleTestAppPage.init();

        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Init Success (Authenticated)");
        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Auth Success");

        jsConsoleTestAppPage.logOut();
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);
        waitUntilElement(jsConsoleTestAppPage.getInitButtonElement()).is().present();
        jsConsoleTestAppPage.init();

        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Init Success (Not Authenticated)");
    }

    @Test
    public void testRefreshToken() {
        jsConsoleTestAppPage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);

        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.refreshToken();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Failed to refresh token");

        jsConsoleTestAppPage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);
        jsConsoleTestAppPage.init();
        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Auth Success");

        jsConsoleTestAppPage.refreshToken();
        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Auth Refresh Success");
    }

    @Test
    public void testRefreshTokenIfUnder30s() {
        jsConsoleTestAppPage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);
        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.refreshToken();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Failed to refresh token");

        jsConsoleTestAppPage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);
        jsConsoleTestAppPage.init();
        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Auth Success");

        jsConsoleTestAppPage.refreshTokenIfUnder30s();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Token not refreshed, valid for");

        pause((TOKEN_LIFESPAN_LEEWAY + 2) * 1000);

        jsConsoleTestAppPage.refreshTokenIfUnder30s();
        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Auth Refresh Success");
    }

    @Test
    public void testGetProfile() {
        jsConsoleTestAppPage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);

        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.getProfile();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Failed to load profile");

        jsConsoleTestAppPage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleTestAppPage);
        jsConsoleTestAppPage.init();
        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Auth Success");

        jsConsoleTestAppPage.getProfile();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("\"username\": \"user\"");
    }

    @Test
    public void testCertEndpoint() {
        logInAndInit("standard");
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Init Success (Authenticated)");

        jsConsoleTestAppPage.sendCertRequest();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Success");
    }

    @Test
    public void grantBrowserBasedApp() {
        testRealmPage.setAuthRealm(EXAMPLE);
        testRealmLoginPage.setAuthRealm(EXAMPLE);
        configPage.setConsoleRealm(EXAMPLE);
        loginEventsPage.setConsoleRealm(EXAMPLE);
        applicationsPage.setAuthRealm(EXAMPLE);

        jsConsoleTestAppPage.navigateTo();
        driver.manage().deleteAllCookies();

        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "js-console");
        ClientRepresentation client = clientResource.toRepresentation();
        client.setConsentRequired(true);
        clientResource.update(client);

        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("REVOKE_GRANT", "LOGIN"));
        testRealmResource().update(realm);

        jsConsoleTestAppPage.navigateTo();
        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.logIn();

        testRealmLoginPage.form().login("user", "password");

        assertTrue(oAuthGrantPage.isCurrent());
        oAuthGrantPage.accept();

        jsConsoleTestAppPage.init();

        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Init Success (Authenticated)");

        applicationsPage.navigateTo();
        applicationsPage.revokeGrantForApplication("js-console");

        jsConsoleTestAppPage.navigateTo();
        jsConsoleTestAppPage.setOnLoad("login-required");
        jsConsoleTestAppPage.init();

        waitUntilElement(By.tagName("body")).is().visible();
        assertTrue(oAuthGrantPage.isCurrent());

        loginEventsPage.navigateTo();
        loginPage.form().login(adminUser);
        loginEventsPage.table().filter();
        loginEventsPage.table().filterForm().addEventType("REVOKE_GRANT");
        loginEventsPage.table().update();

        List<WebElement> resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='REVOKE_GRANT']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='account']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='revoked_client']/../td[text()='js-console']"));

        loginEventsPage.table().reset();
        loginEventsPage.table().filterForm().addEventType("LOGIN");
        loginEventsPage.table().update();
        resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='LOGIN']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='js-console']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='username']/../td[text()='user']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='consent']/../td[text()='consent_granted']"));
    }


    @Test
    public void implicitFlowTest() {
        jsConsoleTestAppPage.navigateTo();
        jsConsoleTestAppPage.setFlow("implicit");
        jsConsoleTestAppPage.init();

        jsConsoleTestAppPage.logIn();
        assertResponseError("Implicit flow is disabled for the client");

        setImplicitFlowForClient();

        jsConsoleTestAppPage.navigateTo();
        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.logIn();
        assertResponseError("Standard flow is disabled for the client");

        logInAndInit("implicit");

        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Init Success (Authenticated)");
    }

    @Test
    public void implicitFlowQueryTest() {
        setImplicitFlowForClient();

        jsConsoleTestAppPage.navigateTo();
        jsConsoleTestAppPage.setFlow("implicit");
        jsConsoleTestAppPage.setResponseMode("query");
        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.logIn();
        assertResponseError("Response_mode 'query' not allowed");
    }

    @Test
    public void implicitFlowRefreshTokenTest() {
        setImplicitFlowForClient();

        logInAndInit("implicit");

        jsConsoleTestAppPage.refreshToken();

        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Failed to refresh token");
    }

    @Test
    public void implicitFlowOnTokenExpireTest() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setAccessTokenLifespanForImplicitFlow(5);
        testRealmResource().update(realm);

        setImplicitFlowForClient();

        logInAndInit("implicit");

        pause(6000);

        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Access token expired");
    }

    @Test
    public void implicitFlowCertEndpoint() {
        setImplicitFlowForClient();
        logInAndInit("implicit");
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Init Success (Authenticated)");

        jsConsoleTestAppPage.sendCertRequest();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Success");
    }

    @Test
    public void testBearerRequest() {
        jsConsoleTestAppPage.navigateTo();
        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.createBearerRequest();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Unauthorized");

        logInAndInit("standard", "unauthorized");
        jsConsoleTestAppPage.createBearerRequest();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Forbidden");

        jsConsoleTestAppPage.logOut();
        logInAndInit("standard");
        jsConsoleTestAppPage.createBearerRequest();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("[\"Bill Burke\",\"Stian Thorgersen\",\"Stan Silvert\",\"Gabriel Cardoso\",\"Viliam Rockai\",\"Marek Posolda\",\"Boleslaw Dawidowicz\"]");
    }

    @Test
    public void loginRequiredAction() {
        jsConsoleTestAppPage.navigateTo();
        jsConsoleTestAppPage.setOnLoad("login-required");
        jsConsoleTestAppPage.init();

        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login("user", "password");

        waitUntilElement(jsConsoleTestAppPage.getInitButtonElement()).is().present();
        jsConsoleTestAppPage.init();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Init Success (Authenticated)");
    }

    @Test
    public void testUpdateToken() {
        logInAndInit("standard");

        jsConsoleTestAppPage.setTimeSkewOffset(-33);
        setTimeOffset(33);

        jsConsoleTestAppPage.refreshTokenIfUnder5s();

        jsConsoleTestAppPage.setTimeSkewOffset(-34);
        setTimeOffset(67);

        jsConsoleTestAppPage.refreshTokenIfUnder5s();
        jsConsoleTestAppPage.createBearerRequestToKeycloak();
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Success");
    }

    @Test
    public void timeSkewTest() {
        logInAndInit("standard");

        jsConsoleTestAppPage.refreshTimeSkew();

        waitUntilElement(jsConsoleTestAppPage.getTimeSkewValue()).text().not().contains("undefined");

        int timeSkew = Integer.parseInt(jsConsoleTestAppPage.getTimeSkewValue().getText());
        assertTrue("TimeSkew was: " + timeSkew + ", but should be ~0", timeSkew >= 0 - TIME_SKEW_TOLERANCE);
        assertTrue("TimeSkew was: " + timeSkew + ", but should be ~0", timeSkew  <= TIME_SKEW_TOLERANCE);

        setTimeOffset(40);
        jsConsoleTestAppPage.refreshToken();
        jsConsoleTestAppPage.refreshTimeSkew();

        waitUntilElement(jsConsoleTestAppPage.getTimeSkewValue()).text().not().contains("undefined");

        timeSkew = Integer.parseInt(jsConsoleTestAppPage.getTimeSkewValue().getText());
        assertTrue("TimeSkew was: " + timeSkew + ", but should be ~-40", timeSkew + 40 >= 0 - TIME_SKEW_TOLERANCE);
        assertTrue("TimeSkew was: " + timeSkew + ", but should be ~-40", timeSkew + 40  <= TIME_SKEW_TOLERANCE);
    }

    // KEYCLOAK-4179
    @Test
    public void testOneSecondTimeSkewTokenUpdate() {
        setTimeOffset(1);

        logInAndInit("standard");

        jsConsoleTestAppPage.refreshToken();

        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Auth Refresh Success");
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().not().contains("Failed to refresh token");

        try {
            // The events element should contain "Auth logout" but we need to wait for it
            // and text().not().contains() doesn't wait. With KEYCLOAK-4179 it took some time for "Auth Logout" to be present
            waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Auth Logout");

            throw new RuntimeException("The events element shouldn't contain \"Auth Logout\" text");
        } catch (TimeoutException e) {
            // OK
        }

    }

    @Test
    public void testLocationHeaderInResponse() {
        logInAndInit("standard");
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text().contains("Init Success (Authenticated)");

        jsConsoleTestAppPage.createUserRequest();

        UsersResource userResource = testRealmResource().users();

        List<UserRepresentation> users = userResource.search("mhajas", 0, 1);
        assertEquals("There should be created user mhajas", 1, users.size());
        waitUntilElement(jsConsoleTestAppPage.getOutputElement()).text()
                .contains("location: " + authServerContextRootPage.toString() + "/auth/admin/realms/" + EXAMPLE + "/users/" + users.get(0).getId());
    }

    @Test
    public void reentrancyCallbackTest() {
        logInAndInit("standard");

        jsConsoleTestAppPage.callReentrancyCallback();

        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("First callback");
        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().contains("Second callback");

        waitUntilElement(jsConsoleTestAppPage.getEventsElement()).text().not().contains("Auth Logout");
    }

    private void setImplicitFlowForClient() {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "js-console");
        ClientRepresentation client = clientResource.toRepresentation();
        client.setImplicitFlowEnabled(true);
        client.setStandardFlowEnabled(false);
        clientResource.update(client);
    }

    private void logInAndInit(String flow, String user) {
        jsConsoleTestAppPage.navigateTo();
        jsConsoleTestAppPage.setFlow(flow);
        jsConsoleTestAppPage.init();
        jsConsoleTestAppPage.logIn();
        waitUntilElement(By.xpath("//body")).is().present();
        testRealmLoginPage.form().login(user, "password");
        jsConsoleTestAppPage.setFlow(flow);
        jsConsoleTestAppPage.init();
    }

    private void logInAndInit(String flow) {
        logInAndInit(flow, "user");
    }

    private void assertResponseError(String errorDescription) {
        jsConsoleTestAppPage.showErrorResponse();
        assertThat(jsConsoleTestAppPage.getOutputElement().getText(), containsString(errorDescription));
    }

}
