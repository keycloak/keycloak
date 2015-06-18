package org.keycloak.testsuite.adapter.example;

import java.io.IOException;
import org.junit.Before;
import org.keycloak.testsuite.arquillian.Relative;
import static org.keycloak.testsuite.adapter.example.AbstractExamplesAdapterTest.CUSTOMER_PORTAL;
import static org.keycloak.testsuite.adapter.example.AbstractExamplesAdapterTest.DATABASE_SERVICE;
import static org.keycloak.testsuite.adapter.example.AbstractExamplesAdapterTest.PRODUCT_PORTAL;
import org.keycloak.testsuite.arquillian.ControlsContainers;
import org.keycloak.testsuite.arquillian.TargetsContainer;

/**
 *
 * @author tkyjovsk
 */
@ControlsContainers({"keycloak-managed"})
@TargetsContainer("keycloak-managed")
@Relative
public class RelativeExamplesAdapterTest extends AbstractExamplesAdapterTest {

    public RelativeExamplesAdapterTest() {
        super(AUTH_SERVER_BASE_URL);
    }

    private static boolean examplesDeployed = false;

    @Before
    public void deployExamples() throws IOException {
        if (!examplesDeployed) {
            
            importRealm();

            deployer.deploy(CUSTOMER_PORTAL);
            deployer.deploy(PRODUCT_PORTAL);
            deployer.deploy(DATABASE_SERVICE);

            examplesDeployed = true;
        }
    }
    
}
