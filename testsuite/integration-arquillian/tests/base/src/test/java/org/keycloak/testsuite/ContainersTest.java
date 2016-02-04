package org.keycloak.testsuite;

import java.util.List;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;

/**
 *
 * @author tkyjovsk
 */
public class ContainersTest extends AbstractKeycloakTest {

    @ArquillianResource
    ContainerController controller;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    
    @Test
    public void testAuthServer() {

        log.info("AUTH SERVER should be started.");
        assertTrue(controller.isStarted(AuthServerTestEnricher.getAuthServerQualifier()));
        
    }

}
