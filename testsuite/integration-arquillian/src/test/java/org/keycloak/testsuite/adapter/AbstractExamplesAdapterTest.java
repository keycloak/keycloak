package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.loadRealm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.keycloak.testsuite.page.adapter.CustomerPortalExample;
import org.keycloak.testsuite.page.adapter.DatabaseServiceExample;
import org.keycloak.testsuite.page.adapter.ProductPortalExample;
import org.keycloak.testsuite.model.RequiredUserAction;
import org.keycloak.testsuite.model.User;
import org.keycloak.testsuite.page.console.settings.user.UserPage;
import org.keycloak.testsuite.page.console.UpdateAccountPage;
import org.openqa.selenium.By;

public abstract class AbstractExamplesAdapterTest extends AbstractAdapterTest {

    public static final String EXAMPLES_HOME;
    public static final String EXAMPLES_VERSION_SUFFIX;

    static {
        EXAMPLES_HOME = System.getProperty("examples.home", null);
        Assert.assertNotNull(EXAMPLES_HOME, "Property ${examples.home} must bet set.");
        System.out.println(EXAMPLES_HOME);

        EXAMPLES_VERSION_SUFFIX = System.getProperty("examples.version.suffix", null);
        Assert.assertNotNull(EXAMPLES_VERSION_SUFFIX, "Property ${examples.version.suffix} must bet set.");
        System.out.println(EXAMPLES_VERSION_SUFFIX);
    }

    @Page
    private CustomerPortalExample customerPortalExample;
    @Page
    private ProductPortalExample productPortalExample;
    @Page
    private DatabaseServiceExample databaseServiceExample;
	@Page
	private UserPage userPage;
	@Page
	private UpdateAccountPage accountPage;

    protected static WebArchive exampleDeployment(String name) throws IOException {
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"))
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);
    }

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
    public void loadAdapterTestRealmsInto(List<RealmRepresentation> testRealms) {
        File testRealmFile = new File(EXAMPLES_HOME + "/keycloak-examples-" + EXAMPLES_VERSION_SUFFIX
                + "/preconfigured-demo/testrealm.json");
        try {
            testRealms.add(loadRealm(new FileInputStream(testRealmFile)));
        } catch (FileNotFoundException ex) {
            throw new IllegalStateException("Test realm file not found: " + testRealmFile);
        }
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
	@Ignore
	public void testChangePasswordRequiredUserAction() {
		System.out.println("befote password login");
		addRequiredAction(RequiredUserAction.UPDATE_PASSWORD);
		
		customerPortalExample.navigateTo();
		customerPortalExample.customerListing();
		loginPage.login("bburke@redhat.com", "password");
		waitGui().until()
			.element(By.className("kc-feedback-text"))
			.text()
			.equalTo("You need to change your password to activate your account.");
		System.out.println("after password login");
		removeRequiredAction(RequiredUserAction.UPDATE_PASSWORD);
	}
	
	@Test
	@Ignore
	public void testUpdateProfileRequiredUserAction() {
		System.out.println("befote profile login");
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
		System.out.println("after profile login");
		removeRequiredAction(RequiredUserAction.UPDATE_PROFILE);
	}
	
	private void addRequiredAction(RequiredUserAction action) {
		try{
		Thread.sleep(1000);
		loginAsAdmin();
		Thread.sleep(1000);
		navigation.users();
		Thread.sleep(1000);
		User bburke = userPage.findUser("bburke@redhat.com");
		Thread.sleep(1000);
		bburke.addRequiredUserAction(action);
		userPage.updateUser(bburke);
		Thread.sleep(1000);
		logOut();
		Thread.sleep(1000);
		} catch(InterruptedException e) {}
	}
	
	private void removeRequiredAction(RequiredUserAction action) {
		try {Thread.sleep(1000);
		loginAsAdmin();
		Thread.sleep(1000);
		navigation.users();
		Thread.sleep(1000);
		User bburke = userPage.findUser("bburke@redhat.com");
		Thread.sleep(1000);
		bburke.removeRequiredUserAction(action);
		userPage.updateUser(bburke);
		logOut();
		} catch(InterruptedException e) {}
	}
}
