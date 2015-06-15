package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.page.example.CustomerPortalPage;
import org.keycloak.testsuite.ui.util.URL;

public class KeycloakExamplesTest extends AbstractKeycloakAdapterTest {

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
    

    protected static WebArchive exampleDeployment(String name) throws IOException {
        WebArchive exampleDeployment = ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"));

        String keycloakJSONContent = IOUtils.toString(
                exampleDeployment.get("/WEB-INF/keycloak.json")
                .getAsset().openStream(), "UTF-8");

        // replace relative /auth URL with AUTH_SERVER_BASE_URL
        exampleDeployment.add(new StringAsset(
                keycloakJSONContent.replace("/auth", URL.AUTH_SERVER_URL)),
                "/WEB-INF/keycloak.json");

        return exampleDeployment;
    }

    @Deployment(name = CUSTOMER_PORTAL, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive customerPortalExample() throws IOException {
        return exampleDeployment("customer-portal-example");
    }

    @Deployment(name = PRODUCT_PORTAL, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive productPortalExample() throws IOException {
        return exampleDeployment("product-portal-example");
    }

    @Deployment(name = DATABASE_SERVICE, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive databaseServiceExample() throws IOException {
        return exampleDeployment("database-service");
    }

    private static boolean examplesInitialized = false;

    @Before
    public void initializeExamples() throws FileNotFoundException, IOException {
        if (!examplesInitialized) {
            File testRealmFile = new File(EXAMPLES_HOME
                    + "/keycloak-examples-" + EXAMPLES_VERSION_SUFFIX
                    + "/preconfigured-demo/testrealm.json");
            
            // replace relative redirectUris in testrealm.json with APP_SERVER_BASE_URL-based URIs
            String testRealmContent = IOUtils.toString(testRealmFile.toURI());
            FileUtils.writeStringToFile(testRealmFile, testRealmContent
                    .replaceAll("(/.*/\\*)", URL.APP_SERVER_BASE_URL + "$1"));

            importRealm(new FileInputStream(testRealmFile));

            deployer.deploy(CUSTOMER_PORTAL);
            deployer.deploy(PRODUCT_PORTAL);
            deployer.deploy(DATABASE_SERVICE);

            examplesInitialized = true;
        }
    }

    @Test
    @RunAsClient
    public void simpleCustomerPortalTest() throws InterruptedException {
        customerPortalPage.open();
        
        customerPortalPage.customerListing();
        loginPage.login("bburke@redhat.com", "password");

        customerPortalPage.assertCurrentURL();
        customerPortalPage.waitForCustomerListingHeader();

        Assert.assertTrue(driver.getPageSource().contains("Username: bburke@redhat.com"));
        Assert.assertTrue(driver.getPageSource().contains("Bill Burke"));
        Assert.assertTrue(driver.getPageSource().contains("Stian Thorgersen"));
    }

}
