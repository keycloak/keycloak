package org.keycloak.testframework.server;

import java.net.MalformedURLException;
import java.net.URL;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

public class ClusterKeycloakUrls {

    private final ContainerKeycloakCluster cluster;

    public ClusterKeycloakUrls(ContainerKeycloakCluster cluster) {
        this.cluster = cluster;
    }

    public KeycloakUrls getKeycloakUrls(int index) {
        return new KeycloakUrls(cluster.getBaseUrl(index), cluster.getManagementBaseUrl(index));
    }

    public boolean isClusterMode() {
        return cluster != null;
    }

}
