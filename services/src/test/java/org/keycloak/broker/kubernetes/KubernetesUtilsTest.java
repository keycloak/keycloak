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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
