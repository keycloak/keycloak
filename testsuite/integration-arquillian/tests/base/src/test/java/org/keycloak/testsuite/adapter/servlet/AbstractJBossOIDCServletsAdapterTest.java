package org.keycloak.testsuite.adapter.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.keycloak.testsuite.adapter.page.CustomerPortalSubsystem;
import org.keycloak.testsuite.adapter.page.ProductPortalSubsystem;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 * OIDC adapter test specific for JBoss-based containers.
 * @author tkyjovsk
 */
public abstract class AbstractJBossOIDCServletsAdapterTest extends AbstractDemoServletsAdapterTest {

    @Page
    private CustomerPortalSubsystem customerPortalSubsystem;

    @Page
    private ProductPortalSubsystem productPortalSubsystem;

    @Deployment(name = CustomerPortalSubsystem.DEPLOYMENT_NAME)
    protected static WebArchive customerPortalSubsystem() {
        return servletDeployment(CustomerPortalSubsystem.DEPLOYMENT_NAME, CustomerServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = ProductPortalSubsystem.DEPLOYMENT_NAME)
    protected static WebArchive productPortalSubsystem() {
        return servletDeployment(ProductPortalSubsystem.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @Test
    public void testSecureDeployments() {
        customerPortalSubsystem.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertTrue(driver.getPageSource().contains("Bill Burke") && driver.getPageSource().contains("Stian Thorgersen"));

        productPortalSubsystem.navigateTo();
        assertCurrentUrlEquals(productPortalSubsystem);
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("iPhone") && pageSource.contains("iPad"));
    }

}
