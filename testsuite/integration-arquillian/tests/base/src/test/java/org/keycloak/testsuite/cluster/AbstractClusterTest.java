package org.keycloak.testsuite.cluster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.utils.tls.TLSUtils;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.util.WaitUtils.pause;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractClusterTest extends AbstractKeycloakTest {

    // Keep the following constants in sync with arquillian
    public static final String QUALIFIER_AUTH_SERVER_NODE_1 = "auth-server-${auth.server}-backend1";
    public static final String QUALIFIER_AUTH_SERVER_NODE_2 = "auth-server-${auth.server}-backend2";

    @ArquillianResource
    protected ContainerController controller;

    protected static Map<ContainerInfo, Keycloak> backendAdminClients = new HashMap<>();

    protected static Map<ContainerInfo, KeycloakTestingClient> backendTestingClients = new HashMap<>();

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

    // Assume that route like "node6" will have corresponding backend container like "auth-server-wildfly-backend6"
    protected void setCurrentFailNodeForRoute(String nodeName) {
        String route = nodeName.substring(nodeName.lastIndexOf('.') + 1);
        String routeNumber;
        int portSeparator = route.indexOf('-');
        if (portSeparator == -1) {
            routeNumber = route.substring(route.length() - 1);
        } else {
            routeNumber = route.substring(portSeparator - 1, portSeparator);
        }
        currentFailNodeIndex = Integer.parseInt(routeNumber) - 1;
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
        if (!backendTestingClients.containsKey(node)) {
            backendTestingClients.put(node, createTestingClientFor(node));
        }
    }

    protected Keycloak createAdminClientFor(ContainerInfo node) {
        log.info("Initializing admin client for " + node.getContextRoot() + "/auth");
        return Keycloak.getInstance(node.getContextRoot() + "/auth",
                MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID, TLSUtils.initializeTLS());
    }

    protected KeycloakTestingClient createTestingClientFor(ContainerInfo node) {
        log.info("Initializing testing client for " + node.getContextRoot() + "/auth");
        return KeycloakTestingClient.getInstance(node.getContextRoot() + "/auth");
    }

    protected void killBackendNode(ContainerInfo node) {
        backendAdminClients.get(node).close();
        backendAdminClients.remove(node);
        backendTestingClients.get(node).close();
        backendTestingClients.remove(node);
        log.info("Killing backend node: " + node);
        controller.kill(node.getQualifier());
    }

    protected Keycloak getAdminClientFor(ContainerInfo node) {
        Keycloak adminClient = backendAdminClients.get(node);

        if (adminClient == null && node.equals(suiteContext.getAuthServerInfo())) {
            adminClient = this.adminClient;
        }

        return adminClient;
    }

    protected KeycloakTestingClient getTestingClientFor(ContainerInfo node) {
        KeycloakTestingClient testingClient = backendTestingClients.get(node);

        if (testingClient == null && node.equals(suiteContext.getAuthServerInfo())) {
            testingClient = this.testingClient;
        }

        return testingClient;
    }

    @BeforeClass
    public static void enabled() {
        ContainerAssume.assumeClusteredContainer();
    }

    @AfterClass
    public static void closeClients() {
        backendAdminClients.values().forEach(Keycloak::close);
        backendAdminClients.clear();

        backendTestingClients.values().forEach(KeycloakTestingClient::close);
        backendTestingClients.clear();

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
