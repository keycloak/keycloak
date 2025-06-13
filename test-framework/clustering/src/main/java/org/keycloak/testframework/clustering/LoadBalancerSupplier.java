package org.keycloak.testframework.clustering;

import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.ClusteredKeycloakServer;
import org.keycloak.testframework.server.KeycloakServer;

public class LoadBalancerSupplier implements Supplier<LoadBalancer, InjectLoadBalancer> {

    @Override
    public LoadBalancer getValue(InstanceContext<LoadBalancer, InjectLoadBalancer> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);

        if (server instanceof ClusteredKeycloakServer clusteredKeycloakServer) {
            return new LoadBalancer(clusteredKeycloakServer);
        }

        throw new IllegalStateException("Load balancer can only be used with ClusteredKeycloakServer");
    }

    @Override
    public boolean compatible(InstanceContext<LoadBalancer, InjectLoadBalancer> a, RequestedInstance<LoadBalancer, InjectLoadBalancer> b) {
        return true;
    }
}
