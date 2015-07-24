package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.util.RealmUtils.loadRealm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.keycloak.testsuite.adapter.page.CustomerPortalExample;
import org.keycloak.testsuite.adapter.page.DatabaseServiceExample;
import org.keycloak.testsuite.adapter.page.ProductPortalExample;
import org.keycloak.testsuite.model.RequiredUserAction;
import org.keycloak.testsuite.model.User;
import static org.keycloak.testsuite.console.page.Realm.DEMO;
import org.keycloak.testsuite.console.page.settings.user.UserPage;
import org.keycloak.testsuite.page.auth.UpdateAccountPage;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;
import static org.keycloak.testsuite.util.SeleniumUtils.pause;
import org.openqa.selenium.By;

public abstract class AbstractDemoExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private CustomerPortalExample customerPortalExample;
    @Page
    private ProductPortalExample productPortalExample;
    @Page
    private DatabaseServiceExample databaseServiceExample;

    @Page
    private UserPage testRealmUsers;
    @Page
    private UpdateAccountPage accountPage;

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
        testRealm.setConsoleRealm(DEMO);
        testRealmUsers.setConsoleRealm(DEMO);
    }

    @Test
    public void simpleCustomerPortalTest() throws InterruptedException {

        customerPortalExample.navigateTo();
        customerPortalExample.customerListing();

        loginPage.login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(customerPortalExample);
        customerPortalExample.waitForCustomerListingHeader();

        Assert.assertTrue(driver.getPageSource().contains("Username: bburke@redhat.com"));
        Assert.assertTrue(driver.getPageSource().contains("Bill Burke"));
        Assert.assertTrue(driver.getPageSource().contains("Stian Thorgersen"));
    }

    @Test
    public void testChangePasswordRequiredUserAction() {
        addRequiredAction(RequiredUserAction.UPDATE_PASSWORD);

        customerPortalExample.navigateTo();
        customerPortalExample.customerListing();
        loginPage.login("bburke@redhat.com", "password");
        waitGui().until()
                .element(By.className("kc-feedback-text"))
                .text()
                .equalTo("You need to change your password to activate your account.");
        removeRequiredAction(RequiredUserAction.UPDATE_PASSWORD);
    }

    @Test
    public void testUpdateProfileRequiredUserAction() {
        addRequiredAction(RequiredUserAction.UPDATE_PROFILE);

        customerPortalExample.navigateTo();
        customerPortalExample.customerListing();
        loginPage.login("bburke@redhat.com", "password");
        waitGui().until()
                .element(By.className("kc-feedback-text"))
                .text()
                .equalTo("You need to update your user profile to activate your account.");
        accountPage.updateAccountInfo("bburke@redhat.com", "Bill", "");
        waitGui().until()
                .element(By.className("kc-feedback-text"))
                .text()
                .equalTo("Please specify last name.");
        accountPage.updateAccountInfo("bburke@redhat.com", "Bill", "Burke");
        waitGui().until()
                .element(By.tagName("h2"))
                .text()
                .equalTo("Customer Listing");
        driver.findElement(By.linkText("logout")).click();
        removeRequiredAction(RequiredUserAction.UPDATE_PROFILE);
    }

    private void addRequiredAction(RequiredUserAction action) {
        loginAsAdmin();
        pause(2000);
        testRealm.navigateTo();
        testRealm.clickUsers();
        pause(1000);
        assertCurrentUrl(testRealmUsers);
        User bburke = testRealmUsers.findUser("bburke@redhat.com");
        pause(1000);
        bburke.addRequiredUserAction(action);
        testRealmUsers.updateUser(bburke);
        pause(1000);
        logOut();
        pause(1000);
    }

    private void removeRequiredAction(RequiredUserAction action) {
        loginAsAdmin();
        pause(2000);
        testRealm.navigateTo();
        testRealm.clickUsers();
        pause(1000);
        User bburke = testRealmUsers.findUser("bburke@redhat.com");
        pause(1000);
        bburke.removeRequiredUserAction(action);
        testRealmUsers.updateUser(bburke);
        logOut();
    }
}
