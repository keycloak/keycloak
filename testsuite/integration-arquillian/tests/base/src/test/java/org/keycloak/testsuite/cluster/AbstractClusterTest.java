package org.keycloak.testsuite.cluster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractClusterTest extends AbstractKeycloakTest {

    @ArquillianResource
    protected ContainerController controller;

    protected Map<ContainerInfo, Keycloak> backendAdminClients = new HashMap<>();

    private int currentFailNodeIndex = 0;

    public int getClusterSize() {
        return suiteContext.getAuthServerBackendsInfo().size();
    }

    protected void iterateCurrentFailNode() {
        currentFailNodeIndex++;
        if (currentFailNodeIndex >= getClusterSize()) {
            currentFailNodeIndex = 0;
        }
        logFailoverSetup();
    }

    protected ContainerInfo getCurrentFailNode() {
        return backendNode(currentFailNodeIndex);
    }

    protected Set<ContainerInfo> getCurrentSurvivorNodes() {
        Set<ContainerInfo> survivors = new HashSet<>(suiteContext.getAuthServerBackendsInfo());
        survivors.remove(getCurrentFailNode());
        return survivors;
    }

    protected void logFailoverSetup() {
        log.info("Current failover setup");
        boolean started = controller.isStarted(getCurrentFailNode().getQualifier());
        log.info("Fail node: " + getCurrentFailNode() + (started ? "" : " (stopped)"));
        for (ContainerInfo survivor : getCurrentSurvivorNodes()) {
            started = controller.isStarted(survivor.getQualifier());
            log.info("Survivor:  " + survivor + (started ? "" : " (stopped)"));
        }
    }

    public void failure() {
        log.info("Simulating failure");
        killBackendNode(getCurrentFailNode());
    }

    public void failback() {
        log.info("Bringing all backend nodes online");
        for (ContainerInfo node : suiteContext.getAuthServerBackendsInfo()) {
            startBackendNode(node);
        }
    }

    protected ContainerInfo frontendNode() {
        return suiteContext.getAuthServerInfo();
    }

    protected ContainerInfo backendNode(int i) {
        return suiteContext.getAuthServerBackendsInfo().get(i);
    }

    protected void startBackendNode(ContainerInfo node) {
        if (!controller.isStarted(node.getQualifier())) {
            log.info("Starting backend node: " + node);
            controller.start(node.getQualifier());
            assertTrue(controller.isStarted(node.getQualifier()));
        }
        log.info("Backend node " + node + " is started");
        if (!backendAdminClients.containsKey(node)) {
            backendAdminClients.put(node, createAdminClientFor(node));
        }
    }

    protected Keycloak createAdminClientFor(ContainerInfo node) {
        log.info("Initializing admin client for " + node.getContextRoot() + "/auth");
        return Keycloak.getInstance(node.getContextRoot() + "/auth",
                MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }

    protected void killBackendNode(ContainerInfo node) {
        backendAdminClients.get(node).close();
        backendAdminClients.remove(node);
        log.info("Killing backend node: " + node);
        controller.kill(node.getQualifier());
    }

    protected Keycloak getAdminClientFor(ContainerInfo node) {
        return node.equals(suiteContext.getAuthServerInfo())
                ? adminClient // frontend client
                : backendAdminClients.get(node);
    }

    @Before
    public void beforeClusterTest() {
        failback();
        logFailoverSetup();
        pause(3000);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        // no test realms will be created by the default 
    }

}
