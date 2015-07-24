package org.keycloak.testsuite.adapter;

import java.io.File;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.util.RealmUtils.loadRealm;
import static org.keycloak.testsuite.adapter.AbstractExampleAdapterTest.EXAMPLES_HOME_DIR;
import org.keycloak.testsuite.adapter.page.fuse.CustomerPortalFuseExample;
import static org.keycloak.testsuite.console.page.Realm.DEMO;
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
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation fureRealm = loadRealm(new File(EXAMPLES_HOME_DIR + "/fuse/testrealm.json"));
        testRealms.add(fureRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealm.setConsoleRealm(DEMO);
    }

//  // no deployments via arquillian - examples already pre-installed by the maven profile    
//    @Deployment(name = CustomerPortalFuseExample.DEPLOYMENT_NAME)
//    public static WebArchive customerPortalFuseExample() throws IOException {
//        return exampleDeployment(CustomerPortalFuseExample.DEPLOYMENT_NAME);
//    }
//
//    @Deployment(name = ProductPortalFuseExample.DEPLOYMENT_NAME, testable = false)
//    public static JavaArchive productPortalFuseExample() {
//        return exampleJarDeployment(ProductPortalFuseExample.DEPLOYMENT_NAME);
//    }

    @Test
    public void testAppServerAvailable() {
        customerPortalFuseExample.navigateTo();
        assertCurrentUrlStartsWith(customerPortalFuseExample);

        pause(10000);
    }

}
