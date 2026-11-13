package org.keycloak.broker.kubernetes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "https://kubernetes.default.svc, https://kubernetes.default.svc/.well-known/openid-configuration",
            "https://kubernetes.default.svc/something/, https://kubernetes.default.svc/something/.well-known/openid-configuration",
            "https://kubernetes.default.svc/something, https://kubernetes.default.svc/something/.well-known/openid-configuration"
    })
    void discoveryUrlAppendsWellKnownPath(String issuer, String expectedDiscoveryUrl) {
        assertEquals(expectedDiscoveryUrl, KubernetesUtils.discoveryUrl(issuer));
    }

    @Test
    void discoveryUrlKeepsFullDiscoveryUrlUnchanged() {
        assertEquals("https://kubernetes.default.svc/.well-known/openid-configuration",
                KubernetesUtils.discoveryUrl("https://kubernetes.default.svc/.well-known/openid-configuration"));
    }

    @Test
    void getServiceAccountTokenReadsAndTrimsToken(@TempDir Path tempDir) throws IOException {
        Path tokenFile = tempDir.resolve("token");
        Files.writeString(tokenFile, "  token-value  \n");

        assertEquals("token-value", KubernetesUtils.getServiceAccountToken(tokenFile.toFile()));
    }

    @Test
    void getServiceAccountTokenReturnsNullForMissingOrEmptyToken(@TempDir Path tempDir) throws IOException {
        Path tokenFile = tempDir.resolve("token");

        assertNull(KubernetesUtils.getServiceAccountToken(tokenFile.toFile()));

        Files.writeString(tokenFile, "  \n");
        assertNull(KubernetesUtils.getServiceAccountToken(tokenFile.toFile()));
    }

    @Test
    void getServiceAccountTokenChecksIssuer(@TempDir Path tempDir) throws IOException {
        String token = token("https://kubernetes.example.test");
        Path tokenFile = tempDir.resolve("token");
        Files.writeString(tokenFile, token);

        assertEquals(token, KubernetesUtils.getServiceAccountToken("https://kubernetes.example.test", tokenFile.toFile()));
        assertNull(KubernetesUtils.getServiceAccountToken("https://other.example.test", tokenFile.toFile()));
    }

    private String token(String issuer) {
        return new JWSBuilder().jsonContent(new JsonWebToken().issuer(issuer)).none();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://kubernetes.default.svc",
            "https://kubernetes.default.svc/openid/v1/jwks",
            "https://kubernetes.default.svc.cluster.local",
            "https://kubernetes.default.svc.cluster.local/openid/v1/jwks",
            "https://kubernetes.default",
            "https://kubernetes.default/keys",
            "https://KUBERNETES.DEFAULT.SVC/jwks",
            "https://kubernetes.default.svc:443/openid/v1/jwks"
    })
    void isTrustedKubernetesEndpointReturnsTrueForTrustedEndpoints(String uri) {
        assertTrue(KubernetesUtils.isTrustedKubernetesEndpoint(uri));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://kubernetes/openid/v1/jwks",
            "https://KUBERNETES:443/openid/v1/jwks",
            "http://kubernetes.default.svc/openid/v1/jwks",
            "https://oidc.eks.us-east-1.amazonaws.com/id/12345/keys",
            "https://login.microsoftonline.com/common/discovery/v2.0/keys",
            "https://accounts.google.com/.well-known/openid-configuration",
            "https://example.com/jwks",
            "https://kubernetes.default.svc:6443/openid/v1/jwks",
            "https://kubernetes.default.svc:8443/openid/v1/jwks",
            "https://kubernetes.default.svc:80/openid/v1/jwks",
            "not-a-valid-uri"
    })
    void isTrustedKubernetesEndpointReturnsFalseForUntrustedEndpoints(String uri) {
        assertFalse(KubernetesUtils.isTrustedKubernetesEndpoint(uri));
    }

    @Test
    void isTrustedKubernetesEndpointAllowsConfiguredApiServiceAddressOnlyOnItsHttpsPort() {
        assertTrue(KubernetesUtils.isTrustedKubernetesEndpoint("https://10.0.0.1:8443/jwks", "10.0.0.1", "8443"));
        assertFalse(KubernetesUtils.isTrustedKubernetesEndpoint("https://10.0.0.1:6443/jwks", "10.0.0.1", "8443"));
        assertFalse(KubernetesUtils.isTrustedKubernetesEndpoint("https://10.0.0.2:8443/jwks", "10.0.0.1", "8443"));
        assertFalse(KubernetesUtils.isTrustedKubernetesEndpoint("https://10.0.0.1:8443/jwks", "10.0.0.1", "invalid"));
        assertFalse(KubernetesUtils.isTrustedKubernetesEndpoint("https://kubernetes.default.svc:8443/jwks", null, null));
    }

    @Test
    void isTrustedKubernetesEndpointReturnsFalseForNullOrEmpty() {
        assertFalse(KubernetesUtils.isTrustedKubernetesEndpoint(null));
        assertFalse(KubernetesUtils.isTrustedKubernetesEndpoint(""));
        assertFalse(KubernetesUtils.isTrustedKubernetesEndpoint("   "));
    }
}
