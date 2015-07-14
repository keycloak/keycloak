package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.loadRealm;
import static org.keycloak.testsuite.adapter.AbstractExampleAdapterTest.EXAMPLES_HOME_DIR;
import org.keycloak.testsuite.page.adapter.fuse.CustomerPortalFuseExample;
import org.keycloak.testsuite.page.adapter.fuse.ProductPortalFuseExample;
import static org.keycloak.testsuite.page.console.Realm.DEMO;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.SeleniumUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractFuseExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    protected CustomerPortalFuseExample customerPortalFuseExample;

    @Override
    public void loadAdapterTestRealmsTo(List<RealmRepresentation> testRealms) {
        RealmRepresentation fureRealm = loadRealm(new File(EXAMPLES_HOME_DIR + "/fuse/testrealm.json"));
        testRealms.add(fureRealm);
    }

    @Override
    public void setPageUriTemplateValues() {
        super.setPageUriTemplateValues();
        testRealm.setTemplateValues(DEMO);
    }

    @Deployment(name = CustomerPortalFuseExample.DEPLOYMENT_NAME)
    public static WebArchive customerPortalFuseExample() throws IOException {
        return exampleDeployment(CustomerPortalFuseExample.DEPLOYMENT_NAME);
    }

    @Deployment(name = ProductPortalFuseExample.DEPLOYMENT_NAME, testable = false)
    public static JavaArchive productPortalFuseExample() {
        return exampleJarDeployment(ProductPortalFuseExample.DEPLOYMENT_NAME);
    }

    @Test
    @Ignore
    public void testAppServerAvailable() {
        customerPortalFuseExample.navigateTo();
        assertCurrentUrlStartsWith(customerPortalFuseExample);

        pause(10000);
    }

}
