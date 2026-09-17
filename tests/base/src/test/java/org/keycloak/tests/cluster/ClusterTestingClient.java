package org.keycloak.tests.cluster;

import java.util.Map;
import java.util.concurrent.Callable;

import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.providers.components.TestComponentProvider;

/**
 * Minimal per-node testing client wrapper for migrated cluster tests.
 */
public class ClusterTestingClient {

    private final Object nodeLock = new Object();
    private final int nodeIndex;
    private final LoadBalancer loadBalancer;
    private final RunOnServerClient runOnServer;

    public ClusterTestingClient(int nodeIndex, LoadBalancer loadBalancer, RunOnServerClient runOnServer) {
        this.nodeIndex = nodeIndex;
        this.loadBalancer = loadBalancer;
        this.runOnServer = runOnServer;
    }

    public ServerEndpoint server() {
        return new ServerEndpoint();
    }

    public TestingEndpoint testing(String realmName) {
        return new TestingEndpoint(realmName);
    }

    public void close() {
        // no-op, lifecycle managed by test framework
    }

    private <T> T onNode(Callable<T> action) {
        synchronized (nodeLock) {
            int previousNodeIndex = loadBalancer.getCurrentNodeIndex();
            try {
                loadBalancer.node(nodeIndex);
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                loadBalancer.node(previousNodeIndex);
            }
        }
    }

    public class ServerEndpoint {

        public void run(RunOnServer function) {
            onNode(() -> {
                runOnServer.run(function);
                return null;
            });
        }

        public <T> T fetch(FetchOnServer function, Class<T> clazz) {
            return onNode(() -> runOnServer.fetch(function, clazz));
        }

        public String fetchString(FetchOnServer function) {
            return onNode(() -> runOnServer.fetchString(function));
        }
    }

    public class TestingEndpoint {

        private final String realmName;

        private TestingEndpoint(String realmName) {
            this.realmName = realmName;
        }

        public Map<String, TestComponentProvider.DetailsRepresentation> getTestComponentDetails() {
            ClusterComponentTestTasks.ComponentDetailsMap details = onNode(() -> runOnServer.fetch(
                    new ClusterComponentTestTasks.AllComponentDetails(realmName),
                    ClusterComponentTestTasks.ComponentDetailsMap.class));
            return details.details();
        }
    }
}
