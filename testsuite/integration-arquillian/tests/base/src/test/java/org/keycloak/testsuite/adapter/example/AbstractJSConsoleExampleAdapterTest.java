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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.JSConsoleExample;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.console.page.events.Config;
import org.keycloak.testsuite.console.page.events.LoginEvents;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.EXAMPLE;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

public abstract class AbstractJSConsoleExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private JSConsoleExample jsConsoleExamplePage;

    @Page
    private Config configPage;

    @Page
    private LoginEvents loginEventsPage;

    @Page
    private OAuthGrant oAuthGrantPage;

    @Page
    private Applications applicationsPage;

    public static int TOKEN_LIFESPAN_LEEWAY = 3; // seconds

    @Deployment(name = JSConsoleExample.DEPLOYMENT_NAME)
    private static WebArchive jsConsoleExample() throws IOException {
        return exampleDeployment(JSConsoleExample.CLIENT_ID);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation jsConsoleRealm = loadRealm(new File(TEST_APPS_HOME_DIR + "/js-console/example-realm.json"));

        fixClientUrisUsingDeploymentUrl(jsConsoleRealm,
                JSConsoleExample.CLIENT_ID, jsConsoleExamplePage.buildUri().toASCIIString());

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
        jsConsoleExamplePage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);

        waitUntilElement(jsConsoleExamplePage.getInitButtonElement()).is().present();

        jsConsoleExamplePage.init();
        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "invalid-password");
        assertCurrentUrlDoesntStartWith(jsConsoleExamplePage);

        testRealmLoginPage.form().login("invalid-user", "password");
        assertCurrentUrlDoesntStartWith(jsConsoleExamplePage);

        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        jsConsoleExamplePage.init();

        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Init Success (Authenticated)");
        waitUntilElement(jsConsoleExamplePage.getEventsElement()).text().contains("Auth Success");

        jsConsoleExamplePage.logOut();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        jsConsoleExamplePage.init();

        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Init Success (Not Authenticated)");
    }

    @Test
    public void testRefreshToken() {
        jsConsoleExamplePage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);

        jsConsoleExamplePage.init();
        jsConsoleExamplePage.refreshToken();
        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Failed to refresh token");

        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        jsConsoleExamplePage.init();
        waitUntilElement(jsConsoleExamplePage.getEventsElement()).text().contains("Auth Success");

        jsConsoleExamplePage.refreshToken();
        waitUntilElement(jsConsoleExamplePage.getEventsElement()).text().contains("Auth Refresh Success");
    }

    @Test
    public void testRefreshTokenIfUnder30s() {
        jsConsoleExamplePage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        jsConsoleExamplePage.init();
        jsConsoleExamplePage.refreshToken();
        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Failed to refresh token");

        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        jsConsoleExamplePage.init();
        waitUntilElement(jsConsoleExamplePage.getEventsElement()).text().contains("Auth Success");

        jsConsoleExamplePage.refreshTokenIfUnder30s();
        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Token not refreshed, valid for");

        pause((TOKEN_LIFESPAN_LEEWAY + 2) * 1000);

        jsConsoleExamplePage.refreshTokenIfUnder30s();
        waitUntilElement(jsConsoleExamplePage.getEventsElement()).text().contains("Auth Refresh Success");
    }

    @Test
    public void testGetProfile() {
        jsConsoleExamplePage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);

        jsConsoleExamplePage.init();
        jsConsoleExamplePage.getProfile();
        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Failed to load profile");

        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        jsConsoleExamplePage.init();
        waitUntilElement(jsConsoleExamplePage.getEventsElement()).text().contains("Auth Success");

        jsConsoleExamplePage.getProfile();
        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("\"username\": \"user\"");
    }

    @Test
    public void grantBrowserBasedApp() {
        testRealmPage.setAuthRealm(EXAMPLE);
        testRealmLoginPage.setAuthRealm(EXAMPLE);
        configPage.setConsoleRealm(EXAMPLE);
        loginEventsPage.setConsoleRealm(EXAMPLE);
        applicationsPage.setAuthRealm(EXAMPLE);

        jsConsoleExamplePage.navigateTo();
        driver.manage().deleteAllCookies();

        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "js-console");
        ClientRepresentation client = clientResource.toRepresentation();
        client.setConsentRequired(true);
        clientResource.update(client);

        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("REVOKE_GRANT", "LOGIN"));
        testRealmResource().update(realm);

        jsConsoleExamplePage.navigateTo();
        jsConsoleExamplePage.init();
        jsConsoleExamplePage.logIn();

        testRealmLoginPage.form().login("user", "password");

        assertTrue(oAuthGrantPage.isCurrent());
        oAuthGrantPage.accept();

        jsConsoleExamplePage.init();

        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Init Success (Authenticated)");

        applicationsPage.navigateTo();
        applicationsPage.revokeGrantForApplication("js-console");

        jsConsoleExamplePage.navigateTo();
        jsConsoleExamplePage.init();
        jsConsoleExamplePage.logIn();

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
        jsConsoleExamplePage.navigateTo();
        jsConsoleExamplePage.setFlow("implicit");
        jsConsoleExamplePage.init();

        jsConsoleExamplePage.logIn();
        assertTrue(driver.getPageSource().contains("Implicit flow is disabled for the client"));

        setImplicitFlowFroClient();

        jsConsoleExamplePage.navigateTo();
        jsConsoleExamplePage.init();
        jsConsoleExamplePage.logIn();
        assertTrue(driver.getPageSource().contains("Standard flow is disabled for the client"));

        logInAndInit("implicit");

        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Init Success (Authenticated)");
    }

    @Test
    public void implicitFlowQueryTest() {
        setImplicitFlowFroClient();

        jsConsoleExamplePage.navigateTo();
        jsConsoleExamplePage.setFlow("implicit");
        jsConsoleExamplePage.setResponseMode("query");
        jsConsoleExamplePage.init();
        jsConsoleExamplePage.logIn();
        assertTrue(driver.getPageSource().contains("Invalid parameter: response_mode"));
    }

    @Test
    public void implicitFlowRefreshTokenTest() {
        setImplicitFlowFroClient();

        logInAndInit("implicit");

        jsConsoleExamplePage.refreshToken();

        waitUntilElement(jsConsoleExamplePage.getOutputElement()).text().contains("Failed to refresh token");
    }

    @Test
    public void implicitFlowOnTokenExpireTest() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setAccessTokenLifespanForImplicitFlow(5);
        testRealmResource().update(realm);

        setImplicitFlowFroClient();

        logInAndInit("implicit");

        pause(6000);

        waitUntilElement(jsConsoleExamplePage.getEventsElement()).text().contains("Access token expired");
    }

    private void setImplicitFlowFroClient() {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "js-console");
        ClientRepresentation client = clientResource.toRepresentation();
        client.setImplicitFlowEnabled(true);
        client.setStandardFlowEnabled(false);
        clientResource.update(client);
    }

    private void logInAndInit(String flow) {
        jsConsoleExamplePage.navigateTo();
        jsConsoleExamplePage.setFlow(flow);
        jsConsoleExamplePage.init();
        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "password");
        jsConsoleExamplePage.setFlow(flow);
        jsConsoleExamplePage.init();
    }

}
