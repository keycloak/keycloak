package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.account.page.Account;
import static org.keycloak.testsuite.util.RealmUtils.loadRealm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.keycloak.testsuite.adapter.page.CustomerPortalExample;
import org.keycloak.testsuite.adapter.page.DatabaseServiceExample;
import org.keycloak.testsuite.adapter.page.ProductPortalExample;
import org.keycloak.testsuite.model.RequiredUserAction;
import static org.keycloak.testsuite.page.auth.AuthRealm.DEMO;
import org.keycloak.testsuite.page.auth.Login;
import org.keycloak.testsuite.page.auth.LoginActions;
import org.openqa.selenium.By;

public abstract class AbstractDemoExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private CustomerPortalExample customerPortalExample;
    @Page
    private ProductPortalExample productPortalExample;
    @Page
    private DatabaseServiceExample databaseServiceExample;

    @Page
    protected Login loginDemo;

    @Page
    private LoginActions demoLoginActions;

    @Page
    private Account account;

    protected RealmResource demoRealmResource;

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
        authRealm.setAuthRealm(DEMO);
    }

    @Before
    public void beforeDemoExampleTest() {
        demoRealmResource = keycloak.realm(DEMO);
        customerPortalExample.navigateTo();
        driver.manage().deleteAllCookies();
    }

    @Test
    public void simpleCustomerPortalTest() throws InterruptedException {

        customerPortalExample.navigateTo();
        customerPortalExample.customerListing();

        loginDemo.login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(customerPortalExample);
        customerPortalExample.waitForCustomerListingHeader();

        Assert.assertTrue(driver.getPageSource().contains("Username: bburke@redhat.com"));
        Assert.assertTrue(driver.getPageSource().contains("Bill Burke"));
        Assert.assertTrue(driver.getPageSource().contains("Stian Thorgersen"));
    }

    @Test
    public void testChangePasswordRequiredUserAction() {
        addRequiredAction("bburke@redhat.com", RequiredUserAction.UPDATE_PASSWORD);

        customerPortalExample.navigateTo();
        customerPortalExample.customerListing();
        loginDemo.login("bburke@redhat.com", "password");
        waitGui().until()
                .element(By.className("kc-feedback-text"))
                .text()
                .equalTo("You need to change your password to activate your account.");
        removeRequiredAction("bburke@redhat.com", RequiredUserAction.UPDATE_PASSWORD);
    }

    @Test
    public void testUpdateProfileRequiredUserAction() {
        addRequiredAction("bburke@redhat.com", RequiredUserAction.UPDATE_PROFILE);

        customerPortalExample.navigateTo();
        customerPortalExample.customerListing();
        loginDemo.login("bburke@redhat.com", "password");
        waitGui().until()
                .element(By.className("kc-feedback-text"))
                .text()
                .equalTo("You need to update your user profile to activate your account.");
        account.setEmail("bburke@redhat.com").setFirstName("Bill").setLastName("");
        demoLoginActions.submit();
        waitGui().until()
                .element(By.className("kc-feedback-text"))
                .text()
                .equalTo("Please specify last name.");
        account.setEmail("bburke@redhat.com").setFirstName("Bill").setLastName("Burke");
        demoLoginActions.submit();
        waitGui().until()
                .element(By.tagName("h2"))
                .text()
                .equalTo("Customer Listing");
        driver.findElement(By.linkText("logout")).click();
        removeRequiredAction("bburke@redhat.com", RequiredUserAction.UPDATE_PROFILE);
    }

    private void addRequiredAction(String username, RequiredUserAction action) {
        UserRepresentation user = findUserByUsername(demoRealmResource, username);
        List<String> ra = user.getRequiredActions();
        if (ra == null) {
            ra = new ArrayList<>();
        }
        ra.add(action.toString());
        keycloak.realm(DEMO).users().get(user.getId()).update(user);
//
//
//        loginAsAdmin();
//        pause(2000);
//        adminConsole.navigateTo();
//        adminConsole.clickUsers();
//        pause(1000);
//        assertCurrentUrl(users);
//        User bburke = users.findUser("bburke@redhat.com");
//        pause(1000);
//        bburke.addRequiredUserAction(action);
//        users.updateUser(bburke);
//        pause(1000);
//        logOut();
//        pause(1000);
    }

    private void removeRequiredAction(String username, RequiredUserAction action) {
        UserRepresentation user = findUserByUsername(demoRealmResource, username);
        List<String> ra = user.getRequiredActions();
        if (ra == null) {
            ra = new ArrayList<>();
        }
        ra.remove(action.toString());
        keycloak.realm(DEMO).users().get(user.getId()).update(user);

//        loginAsAdmin();
//        pause(2000);
//        adminConsole.navigateTo();
//        adminConsole.clickUsers();
//        pause(1000);
//        User bburke = users.findUser("bburke@redhat.com");
//        pause(1000);
//        bburke.removeRequiredUserAction(action);
//        users.updateUser(bburke);
//        logOut();
    }
}
