package org.keycloak.testsuite.adapter.example;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.CustomerPortalExample;
import org.keycloak.testsuite.adapter.page.DatabaseServiceExample;
import org.keycloak.testsuite.adapter.page.ProductPortalExample;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.console.page.clients.settings.ClientSettings;
import org.keycloak.testsuite.console.page.clients.Clients;
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
    private Clients clientsPage;

    @Page
    private ClientSettings clientSettingsPage;

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
        clientsPage.setConsoleRealm(DEMO);
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
        clientsPage.navigateTo();
        loginPage.form().login(adminUser);

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
        loginEventsPage.table().filter();
        loginEventsPage.table().filterForm().addEventType("REVOKE_GRANT");
        loginEventsPage.table().update();

        List<WebElement> resultList = loginEventsPage.table().rows();

        assertEquals(2, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='REVOKE_GRANT']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='account']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='revoked_client']/../td[text()='customer-portal']"));

        loginEventsPage.table().reset();
        loginEventsPage.table().filterForm().addEventType("LOGIN");
        loginEventsPage.table().update();
        resultList = loginEventsPage.table().rows();

        assertEquals(7, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='LOGIN']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='customer-portal']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='username']/../td[text()='bburke@redhat.com']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='consent']/../td[text()='consent_granted']"));
    }
}
