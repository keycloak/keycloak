package org.keycloak.testframework.clustering;

import org.keycloak.testframework.server.ClusteredKeycloakServer;
import org.keycloak.testframework.server.KeycloakUrls;

import java.util.HashMap;

public class LoadBalancer {
    private final ClusteredKeycloakServer server;
    private final HashMap<Integer, KeycloakUrls> urls = new HashMap<>();

    public LoadBalancer(ClusteredKeycloakServer server) {
        this.server = server;
    }

    public KeycloakUrls node(int nodeIndex) {
        if (nodeIndex >= server.clusterSize()) {
            throw new IllegalArgumentException("Node index out of bounds. Requested nodeIndex: %d, cluster size: %d".formatted(server.clusterSize(), nodeIndex));
        }
        return urls.computeIfAbsent(nodeIndex, i -> new KeycloakUrls(server.getBaseUrl(i), server.getManagementBaseUrl(i)));
    }

    public int clusterSize() {
        return server.clusterSize();
    }
}
