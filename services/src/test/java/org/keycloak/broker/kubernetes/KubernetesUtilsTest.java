package org.keycloak.broker.kubernetes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KubernetesUtilsTest {

    @Test
    void discoveryUrlAppendsWellKnownPathToIssuerBaseUrl() {
        assertEquals("https://kubernetes.default.svc/.well-known/openid-configuration",
                KubernetesUtils.discoveryUrl("https://kubernetes.default.svc/"));
    }

    @Test
    void discoveryUrlKeepsFullDiscoveryUrlUnchanged() {
        assertEquals("https://kubernetes.default.svc/.well-known/openid-configuration",
                KubernetesUtils.discoveryUrl("https://kubernetes.default.svc/.well-known/openid-configuration"));
    }
}
