package org.keycloak.testframework;

import java.util.List;

import org.keycloak.testframework.clustering.LoadBalancerSupplier;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.ClusteredKeycloakServerSupplier;

public class ClusteringTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new ClusteredKeycloakServerSupplier(), new LoadBalancerSupplier());
    }
}
