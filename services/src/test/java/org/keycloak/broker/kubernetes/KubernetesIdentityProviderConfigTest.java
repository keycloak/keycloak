package org.keycloak.broker.kubernetes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesIdentityProviderConfigTest {

    @Test
    void automaticIssuerDiscoveryDefaultsToTrueAndParsesCaseInsensitively() {
        KubernetesIdentityProviderConfig config = new KubernetesIdentityProviderConfig();

        assertTrue(config.isAutomaticIssuerDiscovery());

        config.getConfig().put(KubernetesIdentityProviderConfig.AUTOMATIC_ISSUER_DISCOVERY, "FALSE");
        assertFalse(config.isAutomaticIssuerDiscovery());

        config.getConfig().put(KubernetesIdentityProviderConfig.AUTOMATIC_ISSUER_DISCOVERY, "TrUe");
        assertTrue(config.isAutomaticIssuerDiscovery());
    }
}
