package org.keycloak.tests.compatibility;

import java.util.Objects;

import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class ClusteredInvalidationTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectLoadBalancer
    LoadBalancer loadBalancer;

    @AfterEach
    public void cleanup() {
        loadBalancer.node(0);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0"})
    public void testRealmInvalidation(int writer, int reader) {
        // force caching in both nodes
        loadBalancer.node(writer);
        var writerTimeout = Objects.requireNonNullElse(realm.admin().toRepresentation().getClientSessionIdleTimeout(), 0);

        loadBalancer.node(reader);
        var readerTimeout = Objects.requireNonNullElse(realm.admin().toRepresentation().getClientSessionIdleTimeout(), 0);

        assertEquals(writerTimeout, readerTimeout);

        var newTimeout = writerTimeout + 100;

        // write in one of the nodes
        loadBalancer.node(writer);
        realm.updateWithCleanup(r -> r.clientSessionIdleTimeout(newTimeout));

        // should be visible immediately in the writer
        assertEquals(newTimeout, realm.admin().toRepresentation().getClientSessionIdleTimeout());

        loadBalancer.node(reader);
        // Should be visible immediately in the reader.
        // For clusterless the update waits for the other node to consume the event.
        // For non-clusterless, the event is transmitted via the work cache immediately.
        assertEquals(newTimeout, realm.admin().toRepresentation().getClientSessionIdleTimeout());
    }
}
