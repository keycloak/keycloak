package org.keycloak.testsuite.adapter.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractAdapterTest;
import static org.keycloak.testsuite.console.page.PageAssert.assertCurrentUrlStartsWith;
import org.keycloak.testsuite.adapter.page.CustomerPortalExample;

@RunAsClient
public abstract class AbstractExamplesAdapterTest extends AbstractAdapterTest {

    public static final String PRODUCT_PORTAL = "product-portal-example";
    public static final String DATABASE_SERVICE = "database-service-example";

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
    protected CustomerPortalExample customerPortalExample;
    
    // TODO other examples using Pages ^
    

    protected static WebArchive exampleDeployment(String name) throws IOException {
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"));
    }

    @Deployment(name = CustomerPortalExample.DEPLOYMENT_NAME)
    private static WebArchive customerPortalExample() throws IOException {
        return exampleDeployment("customer-portal-example");
    }

    @Deployment(name = PRODUCT_PORTAL)
    private static WebArchive productPortalExample() throws IOException {
        return exampleDeployment("product-portal-example");
    }

    @Deployment(name = DATABASE_SERVICE)
    private static WebArchive databaseServiceExample() throws IOException {
        return exampleDeployment("database-service");
    }

    @Override
    public RealmRepresentation loadTestRealm() {
        File testRealmFile = new File(EXAMPLES_HOME
                + "/keycloak-examples-" + EXAMPLES_VERSION_SUFFIX
                + "/preconfigured-demo/testrealm.json");
        try {
            return loadRealm(new FileInputStream(testRealmFile));
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
