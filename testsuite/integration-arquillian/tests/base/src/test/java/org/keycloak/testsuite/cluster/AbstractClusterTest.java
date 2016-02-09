package org.keycloak.testsuite.cluster;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import static org.junit.Assert.assertTrue;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractClusterTest extends AbstractKeycloakTest {

    @ArquillianResource
    protected ContainerController controller;

    protected List<Keycloak> backendAdminClients = new ArrayList<>();

    public void startBackendNodes(int count) {
        if (count < 0 || count > 10) {
            throw new IllegalArgumentException();
        }
        assertTrue(suiteContext.getAuthServerBackendsInfo().size() >= count);
        for (int i = 0; i < count; i++) {

            ContainerInfo backendNode = suiteContext.getAuthServerBackendsInfo().get(i);

            controller.start(backendNode.getQualifier());
            assertTrue(controller.isStarted(backendNode.getQualifier()));

            backendAdminClients.add(createAdminClientFor(backendNode));
        }
    }
    
    protected Keycloak createAdminClientFor(ContainerInfo backendNode) {
        log.info("Initializing admin client for " + backendNode.getContextRoot() + "/auth");
        return Keycloak.getInstance(backendNode.getContextRoot() + "/auth",
                    MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }

    protected ContainerInfo backendNode(int i) {
        return suiteContext.getAuthServerBackendsInfo().get(i);
    }

    protected void startBackendNode(int i) {
        String container = backendNode(i).getQualifier();
        if (!controller.isStarted(container)) {
            controller.start(container);
            backendAdminClients.set(i, createAdminClientFor(backendNode(i)));
        }
    }

    protected void killBackendNode(int i) {
        backendAdminClients.get(i).close();
        controller.kill(backendNode(i).getQualifier());
    }

}
