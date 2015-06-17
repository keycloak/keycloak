package org.keycloak.testsuite.adapter;

import org.junit.Before;
import org.junit.Ignore;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_DB;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.PRODUCT_PORTAL;
import org.keycloak.testsuite.arquillian.ControlsContainers;
import org.keycloak.testsuite.arquillian.TargetsContainer;

/**
 *
 * @author tkyjovsk
 */
@ControlsContainers({"keycloak-managed"})
@TargetsContainer("keycloak-managed")
@Relative
@Ignore("doesn't work yet")
public class RelativeServletsAdapterTest extends AbstractServletsAdapterTest {

    private static boolean servletsDeployed = false;

    public RelativeServletsAdapterTest() {
        super(AUTH_SERVER_BASE_URL);
    }

    @Before
    public void deployServlets() {
        if (!servletsDeployed) {
            importRealm("/adapter-test/demorealm-relative.json");
            deployer.deploy(CUSTOMER_PORTAL);
            deployer.deploy(CUSTOMER_DB);
            deployer.deploy(PRODUCT_PORTAL);
            servletsDeployed = true;
        }
    }
    
}
