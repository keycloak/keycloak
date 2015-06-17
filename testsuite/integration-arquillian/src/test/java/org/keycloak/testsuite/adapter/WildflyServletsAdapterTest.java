package org.keycloak.testsuite.adapter;

import org.junit.Before;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_DB;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_DB_ERROR_PAGE;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.INPUT_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.PRODUCT_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.SECURE_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.SESSION_PORTAL;
import org.keycloak.testsuite.arquillian.ControlsContainers;
import org.keycloak.testsuite.arquillian.TargetsContainer;

/**
 *
 * @author tkyjovsk
 */
@ControlsContainers({
    "keycloak-managed",
    "wildfly-adapter-managed"})
@TargetsContainer("wildfly-adapter-managed")
public class WildflyServletsAdapterTest extends AbstractServletsAdapterTest {

    public WildflyServletsAdapterTest() {
        super("http://localhost:" + Integer.parseInt(
                System.getProperty("wildfly.http.port", "8080")));
    }
    
    private static boolean servletsDeployed = false;

    @Before
    public void deployServlets() {
        if (!servletsDeployed) {
            importRealm("/adapter-test/demorealm.json");
            deployer.deploy(CUSTOMER_PORTAL);
            deployer.deploy(SECURE_PORTAL);
            deployer.deploy(CUSTOMER_DB);
            deployer.deploy(CUSTOMER_DB_ERROR_PAGE);
            deployer.deploy(PRODUCT_PORTAL);
            deployer.deploy(SESSION_PORTAL);
            deployer.deploy(INPUT_PORTAL);
            servletsDeployed = true;
        }
    }
    
}
