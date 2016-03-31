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

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.CustomerPortalExample;
import org.keycloak.testsuite.adapter.page.DatabaseServiceExample;
import org.keycloak.testsuite.adapter.page.ProductPortalExample;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Account;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

public abstract class AbstractDemoExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private CustomerPortalExample customerPortalExamplePage;

    @Page
    private ProductPortalExample productPortalExamplePage;

    @Page
    private DatabaseServiceExample databaseServiceExamplePage;

    @Page
    private Account testRealmAccountPage;

    @Page
    private Config configPage;

    @Page
    private LoginEvents loginEventsPage;

    @Page
    private OAuthGrant oAuthGrantPage;

    @Page
    private Applications applicationsPage;

    @Deployment(name = CustomerPortalExample.DEPLOYMENT_NAME)
    private static WebArchive customerPortalExample() throws IOException {
        return exampleDeployment(CustomerPortalExample.DEPLOYMENT_NAME);
    }

    @Deployment(name = ProductPortalExample.DEPLOYMENT_NAME)
    private static WebArchive productPortalExample() throws IOException {
        return exampleDeployment(ProductPortalExample.DEPLOYMENT_NAME);
    }

    @Deployment(name = DatabaseServiceExample.DEPLOYMENT_NAME)
    private static WebArchive databaseServiceExample() throws IOException {
        return exampleDeployment("database-service");
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(
                loadRealm(new File(EXAMPLES_HOME_DIR + "/preconfigured-demo/testrealm.json")));
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(DEMO);
        testRealmLoginPage.setAuthRealm(DEMO);
        testRealmAccountPage.setAuthRealm(DEMO);
        configPage.setConsoleRealm(DEMO);
        loginEventsPage.setConsoleRealm(DEMO);
        applicationsPage.setAuthRealm(DEMO);
    }

    @Before
    public void beforeDemoExampleTest() {
        customerPortalExamplePage.navigateTo();
        driver.manage().deleteAllCookies();
        productPortalExamplePage.navigateTo();
        driver.manage().deleteAllCookies();
    }

    @Test
    public void customerPortalListingTest() {

        customerPortalExamplePage.navigateTo();
        customerPortalExamplePage.customerListing();

        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(customerPortalExamplePage);
        customerPortalExamplePage.waitForCustomerListingHeader();

        Assert.assertTrue(driver.getPageSource().contains("Username: bburke@redhat.com"));
        Assert.assertTrue(driver.getPageSource().contains("Bill Burke"));
        Assert.assertTrue(driver.getPageSource().contains("Stian Thorgersen"));
    }

    @Test
    public void customerPortalSessionTest() {

        customerPortalExamplePage.navigateTo();
        customerPortalExamplePage.customerSession();

        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(customerPortalExamplePage);

        customerPortalExamplePage.waitForCustomerSessionHeader();
        Assert.assertTrue(driver.getPageSource().contains("You visited this page"));
    }

    @Test
    public void productPortalListingTest() {

        productPortalExamplePage.navigateTo();
        productPortalExamplePage.productListing();

        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(productPortalExamplePage);
        productPortalExamplePage.waitForProductListingHeader();

        Assert.assertTrue(driver.getPageSource().contains("iphone"));
        Assert.assertTrue(driver.getPageSource().contains("ipad"));
        Assert.assertTrue(driver.getPageSource().contains("ipod"));

        productPortalExamplePage.goToCustomers();
    }

    @Test
    public void goToProductPortalWithOneLoginTest() {

        productPortalExamplePage.navigateTo();
        productPortalExamplePage.productListing();

        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(productPortalExamplePage);
        productPortalExamplePage.waitForProductListingHeader();
        productPortalExamplePage.goToCustomers();

        assertCurrentUrlStartsWith(customerPortalExamplePage);
        customerPortalExamplePage.customerListing();
        customerPortalExamplePage.goToProducts();
        assertCurrentUrlStartsWith(productPortalExamplePage);
    }

    @Test
    public void logoutFromAllAppsTest() {

        productPortalExamplePage.navigateTo();
        productPortalExamplePage.productListing();

        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(productPortalExamplePage);
        productPortalExamplePage.waitForProductListingHeader();

        if (isRelative()) { //KEYCLOAK-1546
            productPortalExamplePage.logOut();
        } else {
            driver.navigate().to(testRealmPage.getOIDCLogoutUrl() + "?redirect_uri=" + productPortalExamplePage);
        }

        assertCurrentUrlStartsWith(productPortalExamplePage);
        productPortalExamplePage.productListing();

        customerPortalExamplePage.navigateTo();
        customerPortalExamplePage.customerListing();
        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        customerPortalExamplePage.logOut();
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

        customerPortalExamplePage.navigateTo();
        customerPortalExamplePage.customerSession();

        loginPage.form().login("bburke@redhat.com", "password");

        assertTrue(oAuthGrantPage.isCurrent());

        oAuthGrantPage.accept();

        assertTrue(driver.getPageSource().contains("Your hostname:"));
        assertTrue(driver.getPageSource().contains("You visited this page"));

        applicationsPage.navigateTo();
        applicationsPage.revokeGrantForApplication("customer-portal");

        customerPortalExamplePage.navigateTo();
        customerPortalExamplePage.customerSession();

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
    }

    @Test
    public void historyOfAccessResourceTest() throws IOException {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("LOGIN", "LOGIN_ERROR", "LOGOUT", "CODE_TO_TOKEN"));
        testRealmResource().update(realm);

        customerPortalExamplePage.navigateTo();
        customerPortalExamplePage.customerListing();

        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        Assert.assertTrue(driver.getPageSource().contains("Username: bburke@redhat.com")
                        && driver.getPageSource().contains("Bill Burke")
                        && driver.getPageSource().contains("Stian Thorgersen")
        );

        if (isRelative()) { //KEYCLOAK-1546
            productPortalExamplePage.logOut();
        } else {
            driver.navigate().to(testRealmPage.getOIDCLogoutUrl() + "?redirect_uri=" + productPortalExamplePage);
        }

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

        String serverLogPath = null;

        if (System.getProperty("app.server").equals("wildfly") || System.getProperty("app.server").equals("eap6") || System.getProperty("app.server").equals("eap")) {
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

            Pattern pattern = Pattern.compile("User '" + bburke.getId() + "' invoking '" + appServerUrl + "customer-portal\\/customers\\/view\\.jsp[^\\s]+' on client 'customer-portal'");
            Matcher matcher = pattern.matcher(serverLogContent);

            assertTrue(matcher.find());
            assertTrue(serverLogContent.contains("User '" + bburke.getId() + "' invoking '" + appServerUrl + "database/customers' on client 'database-service'"));
        } else {
            log.info("Checking app server log on app-server: \"" + System.getProperty("app.server") + "\" is not supported.");
        }
    }
}
