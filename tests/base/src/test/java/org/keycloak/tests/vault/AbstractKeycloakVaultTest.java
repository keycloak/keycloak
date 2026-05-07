package org.keycloak.tests.vault;

import java.net.URL;
import java.util.Optional;

import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.vault.VaultTranscriber;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

abstract class AbstractKeycloakVaultTest {

    @InjectRunOnServer(ref = "first", realmRef = "first")
    RunOnServerClient firstRunOnServer;

    @InjectRunOnServer(ref = "second", realmRef = "second")
    RunOnServerClient secondRunOnServer;

    @Test
    void testVaultInFirstRealm() {
        testVaultResolution(firstRunOnServer, "secure_first_smtp_secret");
    }

    @Test
    void testVaultInSecondRealm() {
        testVaultResolution(secondRunOnServer, "secure_second_smtp_secret");
    }

    protected static String vaultResourcePath(String resource) {
        URL url = AbstractKeycloakVaultTest.class.getResource(resource);
        if (url == null) {
            throw new RuntimeException("Unable to find vault resource: " + resource);
        }
        return url.getPath();
    }

    private static void testVaultResolution(RunOnServerClient client, String expectedSecret) {
        client.run(session -> {
            VaultTranscriber transcriber = session.vault();

            Optional<String> secret = transcriber.getStringSecret("${vault.smtp_key}").get();
            assertThat("vault expression should resolve to realm-specific secret", secret, is(Optional.of(expectedSecret)));

            secret = transcriber.getStringSecret("${vault.invalid_entry}").get();
            assertThat("non-existent vault key should return empty", secret, is(Optional.empty()));

            secret = transcriber.getStringSecret("mysecret").get();
            assertThat("plain string should pass through unchanged", secret, is(Optional.of("mysecret")));
        });
    }
}
