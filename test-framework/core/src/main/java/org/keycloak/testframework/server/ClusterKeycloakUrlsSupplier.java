package org.keycloak.testframework.server;

import org.keycloak.testframework.annotations.InjectClusterKeycloakUrls;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;

public class ClusterKeycloakUrlsSupplier implements Supplier<ClusterKeycloakUrls, InjectClusterKeycloakUrls> {

    @Override
    public ClusterKeycloakUrls getValue(InstanceContext<ClusterKeycloakUrls, InjectClusterKeycloakUrls> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        // TODO investigate if we can do better
        return server instanceof ContainerKeycloakCluster c ?
                new ClusterKeycloakUrls(c) :
                new ClusterKeycloakUrls(null);
    }

    @Override
    public boolean compatible(InstanceContext<ClusterKeycloakUrls, InjectClusterKeycloakUrls> invocationContext, RequestedInstance<ClusterKeycloakUrls, InjectClusterKeycloakUrls> b) {
        return invocationContext.getDependency(KeycloakServer.class) instanceof ContainerKeycloakCluster;
    }
}
