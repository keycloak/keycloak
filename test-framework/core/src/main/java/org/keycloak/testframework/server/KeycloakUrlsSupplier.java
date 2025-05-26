package org.keycloak.testframework.server;

import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;

public class KeycloakUrlsSupplier implements Supplier<KeycloakUrls, InjectKeycloakUrls> {

    @Override
    public KeycloakUrls getValue(InstanceContext<KeycloakUrls, InjectKeycloakUrls> instanceContext) {
        var server = instanceContext.getDependency(KeycloakServer.class);
        var index = instanceContext.getAnnotation().nodeIndex();
        if (index < 0) {
            throw new IllegalArgumentException("@InjectKeycloakUrls nodeIndex must be zero or positive");
        }
        if (index == 0) {
            return new KeycloakUrls(server.getBaseUrl(), server.getManagementBaseUrl());
        }
        if (server instanceof ContainerKeycloakCluster cluster && index < cluster.clusterSize()) {
            return new KeycloakUrls(cluster.getBaseUrl(index), cluster.getManagementBaseUrl(index));
        }
        return new KeycloakUrls(null, null);
    }

    @Override
    public boolean compatible(InstanceContext<KeycloakUrls, InjectKeycloakUrls> a, RequestedInstance<KeycloakUrls, InjectKeycloakUrls> b) {
        return a.getAnnotation().nodeIndex() == b.getAnnotation().nodeIndex();
    }

    @Override
    public String getRef(InjectKeycloakUrls annotation) {
        //TODO: ref to identify the instances, is this correct?
        return "node-" + annotation.nodeIndex();
    }
}
