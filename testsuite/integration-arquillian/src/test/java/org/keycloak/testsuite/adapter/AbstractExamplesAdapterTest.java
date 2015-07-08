package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.loadRealm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.keycloak.testsuite.page.adapter.CustomerPortalExample;
import org.keycloak.testsuite.page.adapter.DatabaseServiceExample;
import org.keycloak.testsuite.page.adapter.ProductPortalExample;

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

}
