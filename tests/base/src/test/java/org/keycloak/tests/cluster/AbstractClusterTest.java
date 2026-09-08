package org.keycloak.tests.cluster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest
public abstract class AbstractClusterTest {

    protected static final Logger log = Logger.getLogger(AbstractClusterTest.class);

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    protected Keycloak adminClient;

    @InjectLoadBalancer
    protected LoadBalancer loadBalancer;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests"})
    protected RunOnServerClient runOnServer;

    private final Map<Integer, ClusterTestingClient> backendTestingClients = new HashMap<>();
    private final Set<Integer> unavailableNodes = new HashSet<>();
    private int currentFailNodeIndex = 0;

    public int getClusterSize() {
        return loadBalancer.nodeCount();
    }

    protected void iterateCurrentFailNode() {
        currentFailNodeIndex++;
        if (currentFailNodeIndex >= getClusterSize()) {
            currentFailNodeIndex = 0;
        }
        logFailoverSetup();
    }

    // Assume that route like "node6" will have corresponding backend container route index 6
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
        Set<ContainerInfo> survivors = new HashSet<>();
        for (int i = 0; i < getClusterSize(); i++) {
            if (i != currentFailNodeIndex && !unavailableNodes.contains(i)) {
                survivors.add(backendNode(i));
            }
        }
        return survivors;
    }

    protected void logFailoverSetup() {
        log.info("Current failover setup");
        boolean started = loadBalancer.isNodeRunning(currentFailNodeIndex);
        log.infof("Fail node: %s%s", getCurrentFailNode(), started ? "" : " (stopped)");
        for (ContainerInfo survivor : getCurrentSurvivorNodes()) {
            started = loadBalancer.isNodeRunning(survivor.getIndex());
            log.infof("Survivor:  %s%s", survivor, started ? "" : " (stopped)");
        }
    }

    public void failure() {
        log.info("Simulating failure");
        killBackendNode(getCurrentFailNode());
    }

    public void failback() {
        log.info("Bringing all backend nodes online");
        for (int i = 0; i < getClusterSize(); i++) {
            if (!loadBalancer.isNodeRunning(i)) {
                loadBalancer.startNode(i);
            }
            unavailableNodes.remove(i);
        }
        if (getClusterSize() > 0) {
            loadBalancer.node(0);
        }
    }

    protected ContainerInfo frontendNode() {
        return backendNode(0);
    }

    protected ContainerInfo backendNode(int i) {
        return new ContainerInfo(i, loadBalancer.nodeUrls(i).getBase());
    }

    protected void startBackendNode(ContainerInfo node) {
        if (!loadBalancer.isNodeRunning(node.getIndex())) {
            loadBalancer.startNode(node.getIndex());
        }
        unavailableNodes.remove(node.getIndex());
        loadBalancer.node(node.getIndex());
        log.infof("Backend node %s is started", node);
    }

    protected void killBackendNode(ContainerInfo node) {
        unavailableNodes.add(node.getIndex());
        backendTestingClients.remove(node.getIndex());
        loadBalancer.stopNode(node.getIndex());

        if (getClusterSize() > 1) {
            int fallback = (node.getIndex() + 1) % getClusterSize();
            if (!unavailableNodes.contains(fallback) && loadBalancer.isNodeRunning(fallback)) {
                loadBalancer.node(fallback);
            }
        }

        log.infof("Backend node %s stopped", node);
    }

    protected Keycloak getAdminClientFor(ContainerInfo node) {
        loadBalancer.node(node.getIndex());
        return adminClient;
    }

    protected ClusterTestingClient getTestingClientFor(ContainerInfo node) {
        return backendTestingClients.computeIfAbsent(node.getIndex(),
                idx -> new ClusterTestingClient(idx, loadBalancer, runOnServer));
    }

    protected void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void beforeClusterTest() {
        failback();
        logFailoverSetup();
        pause(1000);
    }
}
