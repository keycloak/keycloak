package org.keycloak.broker.kubernetes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesUtilsTest {

    @Test
    void trustedApiJwksUrlAllowsApiServerAdvertiseAddressFromTrustedIssuer() {
        assertTrue(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://172.18.0.2:6443/openid/v1/jwks",
                "https://kubernetes.default.svc.cluster.local"));
    }

    @Test
    void trustedApiJwksUrlRejectsExternalHostFromTrustedIssuer() {
        assertFalse(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://oidc.eks.example.com/openid/v1/jwks",
                "https://kubernetes.default.svc.cluster.local"));
    }

    @Test
    void trustedApiJwksUrlRejectsApiServerAdvertiseAddressFromExternalIssuer() {
        assertFalse(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://172.18.0.2:6443/openid/v1/jwks",
                "https://oidc.eks.example.com/id/cluster"));
    }

    @Test
    void trustedApiJwksUrlRejectsNonKubernetesJwksPathFromTrustedIssuer() {
        assertFalse(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://172.18.0.2:6443/external/jwks",
                "https://kubernetes.default.svc.cluster.local"));
    }

    @Test
    void trustedApiJwksUrlAllowsDirectlyTrustedApiUrl() {
        assertTrue(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://kubernetes.default.svc/openid/v1/jwks",
                "https://oidc.eks.example.com/id/cluster"));
    }
}
