package org.keycloak.tests.admin.cli.v2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.client.admin.cli.v2.KcAdmV2Cmd;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.OptionDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.ResourceDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2DescriptorCache;
import org.keycloak.client.cli.util.Headers;
import org.keycloak.client.cli.util.HeadersBody;
import org.keycloak.client.cli.util.HeadersBodyStatus;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.common.Profile;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = KcAdmV2OpenApiFetchCLITest.OpenApiServerConfig.class)
public class KcAdmV2OpenApiFetchCLITest extends AbstractKcAdmV2CLITest {

    @TempDir
    File tempDir;

    @Test
    void testWrongOpenApiUrlFailsHard() {
        Path cacheDir = cacheDir("wrong-url");
        String configFile = configFile("wrong-url.config");

        login(cacheDir, configFile);

        String wrongUrl = keycloakUrls.getBase() + "/wrong/openapi";
        CommandResult result = kcAdmV2Cmd(cacheDir, configFile,
                "config", "openapi", wrongUrl);

        assertThat("should fail when OpenAPI URL is wrong: " + result.err(),
                result.exitCode(), is(not(0)));
        assertThat("error should mention the URL: " + result.err(),
                result.err(), containsString(wrongUrl));
    }

    @Test
    void testOpenApiFetched() throws Exception {
        Path cacheDir = cacheDir("fetched");
        String configFile = configFile("fetched.config");

        CommandResult loginResult = login(cacheDir, configFile);

        assertTrue(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "registry should exist after login (auto-fetched)");
        // we must assert that user is informed where we took the OpenAPI document from
        // as we assume that on the same base URL but with a different port,
        // there runs our Keycloak server management interface, but theoretically, it could be some other service
        assertThat("auto-fetch should report the URL it fetched from: " + loginResult.err(),
                loginResult.err(), containsString("fetched from "));

        // Delete the registry to simulate auto-fetch failure, then verify explicit fetch recovers
        Files.delete(cacheDir.resolve(REGISTRY_FILENAME));
        assertFalse(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "registry should be gone after delete");

        CommandResult fetchResult = fetchOpenApi(cacheDir, configFile);
        assertThat("config openapi should succeed: " + fetchResult.err(),
                fetchResult.exitCode(), is(0));
        assertThat("should confirm descriptor was cached: " + fetchResult.err(),
                fetchResult.err(), containsString("OpenAPI descriptor cached for"));

        assertTrue(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "registry should be recreated by explicit config openapi");

        CommandResult listResult = kcAdmV2Cmd(cacheDir, configFile, "client", "list", "-c");
        assertThat("client list should succeed: " + listResult.err(),
                listResult.exitCode(), is(0));
        assertThat("should return JSON array: " + listResult.out(),
                listResult.out().trim(), startsWith("["));
    }

    @Test
    void testServerSpecificOpenApiUsed() {
        Path cacheDir = cacheDir("server-specific");
        String configFile = configFile("server-specific.config");

        login(cacheDir, configFile);

        KcAdmV2CommandDescriptor descriptor = KcAdmV2Cmd.loadBundledDescriptor();

        OptionDescriptor opt = new OptionDescriptor();
        opt.setName("whatever");
        opt.setFieldName("whatever");
        opt.setType(OptionDescriptor.TYPE_STRING);

        CommandDescriptor fineTestCmd = new CommandDescriptor();
        fineTestCmd.setName("fine-test-operation");
        fineTestCmd.setResourceName("client");
        fineTestCmd.setHttpMethod("POST");
        fineTestCmd.setPath("/no-such-endpoint-at-all");
        fineTestCmd.setDescription("A test operation that does not exist on the server");
        fineTestCmd.setOptions(List.of(opt));

        ResourceDescriptor clientResource = descriptor.getResources().stream()
                .filter(r -> "client".equals(r.getName()))
                .findFirst()
                .orElseThrow();

        List<CommandDescriptor> commands = new ArrayList<>(clientResource.getCommands());
        commands.add(fineTestCmd);
        clientResource.setCommands(commands);

        KcAdmV2DescriptorCache cache = new KcAdmV2DescriptorCache(cacheDir);
        cache.save(keycloakUrls.getBase(), descriptor);

        CommandResult result = kcAdmV2Cmd(cacheDir, configFile,
                "client", "fine-test-operation", "--whatever", "test");
        assertThat("server should reject the fake operation: " + result.err(),
                result.exitCode(), is(not(0)));
        assertThat("server should reject the unknown operation: " + result.err(),
                result.err(), containsString("Unable to find matching target resource method"));
    }

    @Test
    void testOpenApiFetchedFromFile() throws Exception {
        Path cacheDir = cacheDir("from-file");
        String configFile = configFile("from-file.config");

        login(cacheDir, configFile);

        // Fetch raw OpenAPI JSON from the server
        String openApiUrl = managementBaseUrl() + "/openapi";
        HeadersBodyStatus response = HttpUtil.doRequest("get", openApiUrl, new HeadersBody(new Headers()));
        response.checkSuccess();

        // Save to a local file
        Path openApiFile = tempDir.toPath().resolve("openapi-spec.json");
        Files.write(openApiFile, response.getBody().readAllBytes());

        // Delete the registry so we can verify file loading recreates it
        Files.delete(cacheDir.resolve(REGISTRY_FILENAME));
        assertFalse(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "registry should be gone after delete");

        // Load from file via config openapi
        CommandResult result = kcAdmV2Cmd(cacheDir, configFile,
                "config", "openapi", openApiFile.toString());

        assertThat("config openapi from file should succeed: " + result.err(),
                result.exitCode(), is(0));
        assertThat("should confirm descriptor was cached: " + result.err(),
                result.err(), containsString("OpenAPI descriptor cached for"));
        assertTrue(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "registry should be recreated by config openapi from file");

        // Verify the cached descriptor is usable
        CommandResult listResult = kcAdmV2Cmd(cacheDir, configFile, "client", "list", "-c");
        assertThat("client list should succeed with file-loaded descriptor: " + listResult.err(),
                listResult.exitCode(), is(0));
        assertThat("should return JSON array: " + listResult.out(),
                listResult.out().trim(), startsWith("["));
    }

    @Test
    void testOpenApiInvalidFileContent() throws Exception {
        Path cacheDir = cacheDir("invalid-content");
        String configFile = configFile("invalid-content.config");

        login(cacheDir, configFile);

        Path invalidFile = tempDir.toPath().resolve("not-openapi.yaml");
        Files.writeString(invalidFile, """
                openapi: 3.0.0
                info:
                  title: not real
                """);

        CommandResult result = kcAdmV2Cmd(cacheDir, configFile,
                "config", "openapi", invalidFile.toString());

        assertThat("should fail for invalid OpenAPI content: " + result.err(),
                result.exitCode(), is(not(0)));
        assertThat("error should mention the source: " + result.err(),
                result.err(), containsString(invalidFile.toString()));
        assertThat("error should explain why: " + result.err(),
                result.err(), containsString("no resources"));
    }

    @Test
    void testOpenApiInvalidSource() {
        Path cacheDir = cacheDir("invalid-source");
        String configFile = configFile("invalid-source.config");

        login(cacheDir, configFile);

        CommandResult result = kcAdmV2Cmd(cacheDir, configFile,
                "config", "openapi", "/nonexistent/openapi.json");

        assertThat("should fail for invalid source: " + result.err(),
                result.exitCode(), is(not(0)));
        assertThat("error should mention it's not a URL and file not found: " + result.err(),
                result.err(), containsString("no file found at"));
    }

    private CommandResult login(Path cacheDir, String configFile) {
        HttpUtil.clearHttpClient();

        CommandResult result = kcAdmV2Cmd(cacheDir, configFile,
                "config", "credentials",
                "--server", keycloakUrls.getBase(),
                "--realm", "master",
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret());
        assertThat("login should succeed: " + result.err(), result.exitCode(), is(0));
        return result;
    }

    private CommandResult fetchOpenApi(Path cacheDir, String configFile) {
        String openApiUrl = managementBaseUrl() + "/openapi";
        return kcAdmV2Cmd(cacheDir, configFile, "config", "openapi", openApiUrl);
    }

    private Path cacheDir(String name) {
        return tempDir.toPath().resolve(name);
    }

    private String configFile(String name) {
        return new File(tempDir, name).getAbsolutePath();
    }

    public static class OpenApiServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2, Profile.Feature.OPENAPI)
                    .option(OpenApiOptions.OPENAPI_ENABLED.getKey(), "true");
        }
    }
}
