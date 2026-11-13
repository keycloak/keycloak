package org.keycloak.broker.kubernetes;

import org.keycloak.models.IdentityProviderModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesIdentityProviderConfigTest {

    @Test
    void forwardTokenToAdditionalEndpointsDefaultsToFalse() {
        KubernetesIdentityProviderConfig config = new KubernetesIdentityProviderConfig();
        assertFalse(config.isForwardTokenToAdditionalEndpoints());
    }

    @Test
    void setForwardTokenToAdditionalEndpoints() {
        KubernetesIdentityProviderConfig config = new KubernetesIdentityProviderConfig();

        config.setForwardTokenToAdditionalEndpoints(true);
        assertTrue(config.isForwardTokenToAdditionalEndpoints());
        assertEquals("true", config.getConfig().get(KubernetesIdentityProviderConfig.FORWARD_TOKEN_TO_ADDITIONAL_ENDPOINTS));

        config.setForwardTokenToAdditionalEndpoints(false);
        assertFalse(config.isForwardTokenToAdditionalEndpoints());
        assertEquals("false", config.getConfig().get(KubernetesIdentityProviderConfig.FORWARD_TOKEN_TO_ADDITIONAL_ENDPOINTS));
    }

    @Test
    void forwardTokenToAdditionalEndpointsFromModel() {
        IdentityProviderModel model = new IdentityProviderModel();
        model.getConfig().put(KubernetesIdentityProviderConfig.FORWARD_TOKEN_TO_ADDITIONAL_ENDPOINTS, "true");

        KubernetesIdentityProviderConfig config = new KubernetesIdentityProviderConfig(model);
        assertTrue(config.isForwardTokenToAdditionalEndpoints());

        model.getConfig().put(KubernetesIdentityProviderConfig.FORWARD_TOKEN_TO_ADDITIONAL_ENDPOINTS, "invalid");
        config = new KubernetesIdentityProviderConfig(model);
        assertFalse(config.isForwardTokenToAdditionalEndpoints());
    }
}
