package org.keycloak.testsuite.adapter;

import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.adapter.servlet.CallAuthenticatedServlet;
import org.keycloak.testsuite.adapter.servlet.CustomerDatabaseServlet;
import org.keycloak.testsuite.adapter.servlet.CustomerServlet;
import org.keycloak.testsuite.adapter.servlet.ErrorServlet;
import org.keycloak.testsuite.adapter.servlet.ProductServlet;
import org.keycloak.testsuite.adapter.servlet.SessionServlet;
import static org.keycloak.testsuite.ui.util.URL.*;

public class BasicKeycloakAdapterTest extends AbstractKeycloakAdapterTest {

    public static final String CUSTOMER_PORTAL = "customer-portal";
    public static final String SECURE_PORTAL = "secure-portal";
    public static final String CUSTOMER_DB = "customer-db";
    public static final String CUSTOMER_DB_ERROR_PAGE = "customer-db-error-page";
    public static final String PRODUCT_PORTAL = "product-portal";
    public static final String SESSION_PORTAL = "session-portal";
    public static final String INPUT_PORTAL = "input-portal";

    protected static WebArchive adapterDeployment(String name, Class... servletClasses) {
        return adapterDeployment(name, "keycloak.json", servletClasses);
    }

    protected static WebArchive adapterDeployment(String name, String adapterConfig, Class... servletClasses) {
        String webInfPath = "/adapter-test/" + name + "/WEB-INF/";
        URL keycloakJSON = BasicKeycloakAdapterTest.class.getResource(webInfPath + adapterConfig);
        URL webXML = BasicKeycloakAdapterTest.class.getResource(webInfPath + "web.xml");
        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(webXML, "web.xml")
                .addAsWebInfResource(keycloakJSON, "keycloak.json");

        URL jbossDeploymentStructure = BasicKeycloakAdapterTest.class.getResource(webInfPath + "jboss-deployment-structure.xml");
        if (jbossDeploymentStructure != null) {
            deployment = deployment.addAsWebInfResource(jbossDeploymentStructure, "jboss-deployment-structure.xml");
        }
        
        return deployment;
    }

    @Deployment(name = CUSTOMER_PORTAL, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive customerPortal() {
        return adapterDeployment(CUSTOMER_PORTAL, CustomerServlet.class, ErrorServlet.class);
    }

    @Deployment(name = SECURE_PORTAL, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive securePortal() {
        return adapterDeployment(SECURE_PORTAL, CallAuthenticatedServlet.class);
    }

    @Deployment(name = CUSTOMER_DB, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive customerDb() {
        return adapterDeployment(CUSTOMER_DB, CustomerDatabaseServlet.class);
    }

    @Deployment(name = CUSTOMER_DB_ERROR_PAGE, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive customerDbErrorPage() {
        return adapterDeployment(CUSTOMER_DB_ERROR_PAGE, CustomerDatabaseServlet.class, ErrorServlet.class);
    }

    @Deployment(name = PRODUCT_PORTAL, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive productPortal() {
        return adapterDeployment(PRODUCT_PORTAL, ProductServlet.class);
    }

    @Deployment(name = SESSION_PORTAL, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive sessionPortal() {
        return adapterDeployment(SESSION_PORTAL, SessionServlet.class);
    }

    @Deployment(name = INPUT_PORTAL, managed = false, testable = false)
    @TargetsContainer(KEYCLOAK_ADAPTER_SERVER)
    private static WebArchive inputPortal() {
        return adapterDeployment(INPUT_PORTAL, ProductServlet.class);
    }

//    @Before
//    public void setSystemProperties() {
//        // Test that replacing system properties works for adapters
//        System.setProperty("app.server.base.url", "http://localhost:8081");
//        System.setProperty("my.host.name", "localhost");
//    }
    @Before
    public void importDemoRealm() {
        importRealm("/adapter-test/demorealm.json");
    }

    @Test
    @RunAsClient
    public void test1() throws InterruptedException {

        deployer.deploy(CUSTOMER_PORTAL);
        deployer.deploy(SECURE_PORTAL);
        deployer.deploy(CUSTOMER_DB);
        deployer.deploy(CUSTOMER_DB_ERROR_PAGE);
        deployer.deploy(PRODUCT_PORTAL);
//        deployer.deploy(SESSION_PORTAL);
//        deployer.deploy(INPUT_PORTAL);

        driver.get(KEYCLOAK_ADAPTER_SERVER_BASE_URL + "/" + CUSTOMER_PORTAL);
        Thread.sleep(300000);
    }

}
