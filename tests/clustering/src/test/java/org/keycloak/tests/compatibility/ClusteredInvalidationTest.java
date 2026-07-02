package org.keycloak.tests.compatibility;

import java.time.Duration;
import java.util.Objects;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.representations.info.FeatureRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.realm.ManagedRealm;

import org.awaitility.Awaitility;
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

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @AfterEach
    public void cleanup() {
        loadBalancer.node(0);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0"})
    public void testRealmInvalidation(int writer, int reader) {
        var cacheless = isCachelessFeatureEnabled();

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
        if (cacheless) {
            // with cacheless, invalidation takes a while to be propagated.
            // the invalidation logic should poll around 3 or 4 times and the test fails if it didn't process the invalidation message during that period
            Awaitility.await()
                    .atMost(Duration.ofSeconds(1))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> assertEquals(newTimeout, realm.admin().toRepresentation().getClientSessionIdleTimeout()));
        } else {
            // should be visible immediately in the reader
            assertEquals(newTimeout, realm.admin().toRepresentation().getClientSessionIdleTimeout());
        }
    }

    private boolean isCachelessFeatureEnabled() {
        var serverInfo = adminClient.serverInfo().getInfo();
        FeatureRepresentation feature = serverInfo.getFeatures().stream()
                .filter(feat -> Profile.Feature.CACHELESS.name().equals(feat.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cacheless feature not found in server info"));
        return feature.isEnabled();
    }


}
