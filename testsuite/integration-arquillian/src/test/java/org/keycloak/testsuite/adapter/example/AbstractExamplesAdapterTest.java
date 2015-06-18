package org.keycloak.testsuite.adapter.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.testsuite.adapter.AbstractAdapterTest;
import org.keycloak.testsuite.ui.page.example.CustomerPortalPage;

@RunAsClient
public abstract class AbstractExamplesAdapterTest extends AbstractAdapterTest {

    public static final String CUSTOMER_PORTAL = "customer-portal-example";
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
    protected CustomerPortalPage customerPortalPage;

    public AbstractExamplesAdapterTest(String appServerBaseURL) {
        super(appServerBaseURL);
    }

    protected static WebArchive exampleDeployment(String name) throws IOException {
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"));
    }

    @Deployment(name = CUSTOMER_PORTAL, managed = false)
    private static WebArchive customerPortalExample() throws IOException {
        return exampleDeployment("customer-portal-example");
    }

    @Deployment(name = PRODUCT_PORTAL, managed = false)
    private static WebArchive productPortalExample() throws IOException {
        return exampleDeployment("product-portal-example");
    }

    @Deployment(name = DATABASE_SERVICE, managed = false)
    private static WebArchive databaseServiceExample() throws IOException {
        return exampleDeployment("database-service");
    }

    protected void importRealm() throws FileNotFoundException, IOException {
        File testRealmFile = new File(EXAMPLES_HOME
                + "/keycloak-examples-" + EXAMPLES_VERSION_SUFFIX
                + "/preconfigured-demo/testrealm.json");

        if (!isRelative()) {
            // replace relative redirectUris in testrealm.json with APP_SERVER_BASE_URL URIs
            String testRealmContent = IOUtils.toString(testRealmFile.toURI());
            FileUtils.writeStringToFile(testRealmFile, testRealmContent
                    .replaceAll("(/.*/\\*)", APP_SERVER_BASE_URL + "$1"));
        }

        importRealm(new FileInputStream(testRealmFile));
    }

    @Test
    public void simpleCustomerPortalTest() throws InterruptedException {
        driver.get(APP_SERVER_BASE_URL + "/customer-portal");

        customerPortalPage.customerListing();
        loginPage.login("bburke@redhat.com", "password");

        Assert.assertTrue(driver.getCurrentUrl().startsWith(APP_SERVER_BASE_URL + "/customer-portal"));
        customerPortalPage.waitForCustomerListingHeader();

        Assert.assertTrue(driver.getPageSource().contains("Username: bburke@redhat.com"));
        Assert.assertTrue(driver.getPageSource().contains("Bill Burke"));
        Assert.assertTrue(driver.getPageSource().contains("Stian Thorgersen"));
    }

}
