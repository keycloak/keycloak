package org.keycloak.testsuite.adapter.example;

import java.io.IOException;
import org.junit.Before;
import org.keycloak.testsuite.arquillian.AppServerContainer;
import static org.keycloak.testsuite.adapter.example.AbstractExamplesAdapterTest.CUSTOMER_PORTAL;
import static org.keycloak.testsuite.adapter.example.AbstractExamplesAdapterTest.DATABASE_SERVICE;
import static org.keycloak.testsuite.adapter.example.AbstractExamplesAdapterTest.PRODUCT_PORTAL;
import org.keycloak.testsuite.arquillian.AuthServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AuthServerContainer("auth-server-wildfly")
@AppServerContainer("app-server-wildfly")
public class WildflyExamplesAdapterTest extends AbstractExamplesAdapterTest {

    public WildflyExamplesAdapterTest() {
        super("http://localhost:" + Integer.parseInt(
                System.getProperty("wildfly.http.port", "8080")));
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
