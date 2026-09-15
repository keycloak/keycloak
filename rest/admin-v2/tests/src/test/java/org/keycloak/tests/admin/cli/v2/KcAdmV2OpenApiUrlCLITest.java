package org.keycloak.tests.admin.cli.v2;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.common.Profile;
import org.keycloak.config.ManagementOptions;
import org.keycloak.config.OpenApiOptions;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.keycloak.client.admin.cli.v2.KcAdmV2DescriptorCache.REGISTRY_FILENAME;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = KcAdmV2OpenApiUrlCLITest.NonDefaultManagementPortConfig.class)
public class KcAdmV2OpenApiUrlCLITest extends AbstractKcAdmV2CLITest {

    private static final String NON_DEFAULT_MANAGEMENT_PORT = "9004";

    @TempDir
    File tempDir;

    @Test
    void testLoginWithOpenApiUrl() {
        Path cacheDir = tempDir.toPath().resolve("openapi-url");
        String configFile = new File(tempDir, "openapi-url.config").getAbsolutePath();

        HttpUtil.clearHttpClient();

        // Login with --openapi-url pointing to the actual (non-default) management URL
        String openApiUrl = managementOpenApiUrl();
        CommandResult result = kcAdmV2Cmd(cacheDir, configFile,
                "config", "credentials",
                "--server", keycloakUrls.getBase(),
                "--realm", "master",
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret(),
                "--openapi-url", openApiUrl);

        assertThat("login should succeed: " + result.err(), result.exitCode(), is(0));
        assertTrue(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "registry should exist — auto-fetch should use the provided --openapi-url");
        assertThat("should report the URL it fetched from: " + result.err(),
                result.err(), containsString("fetched from " + openApiUrl));

        // Verify the auto-fetched descriptor is usable
        CommandResult listResult = kcAdmV2Cmd(cacheDir, configFile, "client", "list", "-c");
        assertThat("client list should succeed: " + listResult.err(),
                listResult.exitCode(), is(0));
        assertThat("should return JSON array: " + listResult.out(),
                listResult.out().trim(), startsWith("["));
    }

    @Test
    void testLoginWithoutOpenApiUrlFailsOnNonDefaultPort() {
        Path cacheDir = tempDir.toPath().resolve("no-openapi-url");
        String configFile = new File(tempDir, "no-openapi-url.config").getAbsolutePath();

        HttpUtil.clearHttpClient();

        // Login WITHOUT --openapi-url — auto-fetch tries default port 9000, but server is on NON_DEFAULT_MANAGEMENT_PORT
        CommandResult result = kcAdmV2Cmd(cacheDir, configFile,
                "config", "credentials",
                "--server", keycloakUrls.getBase(),
                "--realm", "master",
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret());

        assertThat("login should succeed even when auto-fetch fails: " + result.err(), result.exitCode(), is(0));
        assertFalse(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "no registry should be created — auto-fetch should fail on wrong port");
        assertThat("should warn about auto-fetch failure: " + result.err(),
                result.err(), containsString("Failed to fetch OpenAPI"));

        // Explicit config openapi with the correct URL should recover
        String openApiUrl = managementOpenApiUrl();
        CommandResult fetchResult = kcAdmV2Cmd(cacheDir, configFile,
                "config", "openapi", openApiUrl);
        assertThat("explicit config openapi should succeed: " + fetchResult.err(),
                fetchResult.exitCode(), is(0));
        assertTrue(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "registry should exist after explicit config openapi");

        // Verify the descriptor is usable
        CommandResult listResult = kcAdmV2Cmd(cacheDir, configFile, "client", "list", "-c");
        assertThat("client list should succeed: " + listResult.err(),
                listResult.exitCode(), is(0));
        assertThat("should return JSON array: " + listResult.out(),
                listResult.out().trim(), startsWith("["));
    }

    private String managementOpenApiUrl() {
        // FIXME: drop this when https://github.com/keycloak/keycloak/issues/47673 is fixed
        try {
            URL managementUrl = new URL(managementBaseUrl());
            return managementUrl.getProtocol() + "://" + managementUrl.getHost()
                    + ":" + NON_DEFAULT_MANAGEMENT_PORT + "/openapi";
        } catch (Exception e) {
            throw new AssertionError("Could not parse management base URL", e);
        }
    }

    public static class NonDefaultManagementPortConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2, Profile.Feature.OPENAPI)
                    .option(OpenApiOptions.OPENAPI_ENABLED.getKey(), "true")
                    .option(ManagementOptions.HTTP_MANAGEMENT_PORT.getKey(), NON_DEFAULT_MANAGEMENT_PORT);
        }
    }
}
