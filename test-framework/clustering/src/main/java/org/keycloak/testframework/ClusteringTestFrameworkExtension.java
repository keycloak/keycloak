package org.keycloak.testframework;

import org.keycloak.testframework.clustering.LoadBalancerSupplier;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.ClusteredKeycloakServerSupplier;

import java.util.List;

public class ClusteringTestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new ClusteredKeycloakServerSupplier(), new LoadBalancerSupplier());
    }
}
