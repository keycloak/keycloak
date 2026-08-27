package org.keycloak.broker.kubernetes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void trustedApiUrlAllowsKubernetesServiceHosts() {
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("https://kubernetes", null, null, null));
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("https://kubernetes.default", null, null, null));
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("https://kubernetes.default.svc", null, null, null));
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("https://kubernetes.default.svc.cluster.local", null, null, null));
    }

    @Test
    void trustedApiUrlAllowsConfiguredServiceHostAndHttpsPort() {
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("https://10.0.0.1:8443", "10.0.0.1", "8443", "443"));
        assertFalse(KubernetesUtils.isTrustedKubernetesApiUrl("https://10.0.0.1:443", "10.0.0.1", "8443", "443"));
    }

    @Test
    void trustedApiUrlUsesRegularServicePortWhenHttpsPortIsMissing() {
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("https://10.0.0.1:7443", "10.0.0.1", null, "7443"));
        assertFalse(KubernetesUtils.isTrustedKubernetesApiUrl("https://10.0.0.1:443", "10.0.0.1", null, "7443"));
    }

    @Test
    void trustedApiUrlDefaultsToHttpsPortWhenServicePortsAreMissing() {
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("https://10.0.0.1", "10.0.0.1", null, null));
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("https://10.0.0.1:443", "10.0.0.1", null, null));
        assertFalse(KubernetesUtils.isTrustedKubernetesApiUrl("https://10.0.0.1:8443", "10.0.0.1", null, null));
    }

    @Test
    void trustedApiUrlRejectsNonHttpsUrl() {
        assertFalse(KubernetesUtils.isTrustedKubernetesApiUrl("http://kubernetes.default.svc", null, null, null));
    }

    @Test
    void trustedApiUrlAcceptsCaseInsensitiveHttpsScheme() {
        assertTrue(KubernetesUtils.isTrustedKubernetesApiUrl("HTTPS://kubernetes.default.svc", null, null, null));
    }

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
    void trustedApiJwksUrlRejectsUnexpectedPathOnTrustedApiHost() {
        assertFalse(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://kubernetes.default.svc/api/v1/secrets",
                "https://oidc.eks.example.com/id/cluster"));
    }

    @Test
    void trustedApiJwksUrlRejectsQueryAndFragmentOnTrustedApiHost() {
        assertFalse(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://kubernetes.default.svc/openid/v1/jwks?audience=keycloak",
                "https://oidc.eks.example.com/id/cluster"));
        assertFalse(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://kubernetes.default.svc/openid/v1/jwks#fragment",
                "https://oidc.eks.example.com/id/cluster"));
    }

    @Test
    void trustedApiJwksUrlRejectsUnexpectedPort() {
        assertFalse(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://172.18.0.2:65535/openid/v1/jwks",
                "https://kubernetes.default.svc.cluster.local"));
    }

    @Test
    void trustedApiJwksUrlAllowsDirectlyTrustedApiUrl() {
        assertTrue(KubernetesUtils.isTrustedKubernetesApiJwksUrl(
                "https://kubernetes.default.svc/openid/v1/jwks",
                "https://oidc.eks.example.com/id/cluster"));
    }
}
