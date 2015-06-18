package org.keycloak.testsuite.adapter;

import org.junit.Before;
import org.junit.Ignore;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_DB;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_DB_ERROR_PAGE;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.CUSTOMER_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.INPUT_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.PRODUCT_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.SECURE_PORTAL;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.SESSION_PORTAL;
import org.keycloak.testsuite.arquillian.AuthServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AuthServerContainer("auth-server-undertow")
@Ignore
public class UndertowServletsAdapterTest extends AbstractServletsAdapterTest {

    public UndertowServletsAdapterTest() {
        super("http://localhost:" + Integer.parseInt(
                System.getProperty("undertow.http.port", "8080")));
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
