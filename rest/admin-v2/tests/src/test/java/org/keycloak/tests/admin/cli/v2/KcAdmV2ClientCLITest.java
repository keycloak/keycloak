package org.keycloak.tests.admin.cli.v2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.keycloak.client.admin.cli.v2.KcAdmV2DescriptorCache.REGISTRY_FILENAME;
import static org.keycloak.client.cli.config.FileConfigHandler.setConfigFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KeycloakIntegrationTest(config = KcAdmV2ClientCLITest.V2ApiServerConfig.class)
public class KcAdmV2ClientCLITest extends AbstractKcAdmV2CLITest {

    @InjectRealm
    ManagedRealm realm;

    @TempDir
    static File tempDir;

    private static String configFilePath;

    @TestSetup
    public void login() {
        configFilePath = new File(tempDir, "kcadm.config").getAbsolutePath();
        HttpUtil.clearHttpClient();

        CommandResult result = kcAdmV2Cmd(
                "config", "credentials",
                "--server", keycloakUrls.getBase(),
                "--realm", "master",
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret());

        assertThat("login should succeed: " + result.err(), result.exitCode(), is(0));
    }

    @Test
    void testCreateClientValidationError() {
        CommandResult result = kcAdmV2Cmd("client", "create", "oidc");

        assertThat("create without clientId should fail", result.exitCode(), is(not(0)));
        assertThat("error should clarify that provided data are invalid: ", result.err(), is("""
                Provided data is invalid:
                	- clientId: must not be blank
                """));
    }

    @Test
    void testCreateClientFileNotFound() {
        CommandResult result = kcAdmV2Cmd("client", "create", "oidc", "-f", "/nonexistent/file.json");

        assertThat("create with missing file should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("not found"));
    }

    @Test
    void testCreateWithMalformedJsonFile() throws Exception {
        Path jsonFile = new File(tempDir, "bad.json").toPath();
        Files.writeString(jsonFile, "{ not valid json }");

        CommandResult result = kcAdmV2Cmd("client", "create", "oidc", "-f", jsonFile.toString());

        assertThat("malformed JSON should fail", result.exitCode(), is(not(0)));
        assertThat("should not produce a stack trace",
                result.err(), not(containsString("Exception")));
        assertThat("should tell user about JSON parsing problem",
                result.err(), containsString("Cannot parse the JSON"));
    }

    @Test
    void testGetWithoutIdFails() {
        CommandResult result = kcAdmV2Cmd("client", "get");

        assertThat("get without ID should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Missing required parameter"));
    }

    @Test
    void testGetNonExistentClient() {
        CommandResult result = kcAdmV2Cmd("client", "get", "non-existent-id");

        assertThat("get non-existent client should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Cannot find the specified client"));
    }

    @Test
    void testFileAndFieldOptionsMutuallyExclusive() throws Exception {
        Path jsonFile = new File(tempDir, "exclusive.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "test-exclusive", "protocol": "openid-connect"}
                """);

        CommandResult result = kcAdmV2Cmd("client", "create", "oidc",
                "-f", jsonFile.toString(), "--client-id", "test-exclusive");

        assertThat("should fail when both -f and field options used", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("mutually exclusive"));
    }

    @Test
    void testAuthNestedObjectMerged() {
        CommandResult result = kcAdmV2Cmd("client", "create", "oidc",
                "--client-id", "test-auth-nested",
                "--auth-method", "client_secret",
                "--auth-secret", "my-secret-value");

        assertThat("create should succeed: " + result.err(), result.exitCode(), is(0));

        String id = extractId(result);
        CommandResult getResult = kcAdmV2Cmd("client", "get", id);
        assertThat("get should succeed", getResult.exitCode(), is(0));
        assertThat(getResult.out(), containsString("client_secret"));
        assertThat(getResult.out(), containsString("my-secret-value"));
    }

    @Test
    void testListClients() {
        kcAdmV2Cmd("client", "create", "oidc", "--client-id", "test-for-list-1");
        kcAdmV2Cmd("client", "create", "oidc", "--client-id", "test-for-list-2");

        CommandResult result = kcAdmV2Cmd("client", "list", "-c");
        assertThat("list should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("test-for-list-1"));
        assertThat(result.out(), containsString("test-for-list-2"));
        assertThat("compressed output should not be indented",
                result.out(), not(containsString("  \"")));
    }

    @Test
    void testGetClient() {
        String id = createClientWithAllParams("test-for-get");

        CommandResult result = kcAdmV2Cmd("client", "get", id);

        assertThat("get should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("test-for-get"));
        assertThat(result.out(), containsString("A test client with all params"));
        assertThat(result.out(), containsString("https://example.com/callback"));
        assertThat(result.out(), containsString("https://example.com/logout"));
        assertThat(result.out(), containsString("role1"));
        assertThat(result.out(), containsString("role2"));
        assertThat(result.out(), containsString("STANDARD"));
        assertThat(result.out(), containsString("SERVICE_ACCOUNT"));
        assertThat(result.out(), containsString("client_secret"));
    }

    @Test
    void testPatchClient() {
        CommandResult createResult = kcAdmV2Cmd("client", "create", "oidc",
                "--client-id", "test-for-patch", "--enabled", "true");
        assertThat("setup: create should succeed", createResult.exitCode(), is(0));
        assertThat(createResult.out(), containsString("\"enabled\" : true"));

        String id = extractId(createResult);
        CommandResult patchResult = kcAdmV2Cmd("client", "patch", "oidc", id, "--enabled", "false");
        assertThat("patch should succeed: " + patchResult.err(), patchResult.exitCode(), is(0));
        assertThat(patchResult.out(), containsString("\"enabled\" : false"));
    }

    @Test
    void testRealmOverrideWithMasterConfig() {
        // Config login is for master — targeting another realm should work (master can manage other realms)
        assertThat("managed realm should not be master", realm.getName(), is(not("master")));

        CommandResult result = kcAdmV2Cmd("client", "create", "oidc",
                "-r", realm.getName(),
                "--client-id", "test-in-other-realm");
        assertThat("create in other realm should succeed: " + result.err(),
                result.exitCode(), is(0));

        CommandResult listResult = kcAdmV2Cmd("client", "list", "-r", realm.getName(), "-c");
        assertThat("list should succeed", listResult.exitCode(), is(0));
        assertThat(listResult.out(), containsString("test-in-other-realm"));

        CommandResult masterList = kcAdmV2Cmd("client", "list", "-c");
        assertThat(masterList.out(), not(containsString("test-in-other-realm")));
    }

    @Test
    void testRealmOverrideWithInlineAuth() {
        // Inline auth with --realm uses that realm for authentication.
        // The admin client only exists in master, so authenticating against a non-master realm
        // should fail — proving --realm is actually used for auth, not just the request URL.
        assertThat("managed realm should not be master", realm.getName(), is(not("master")));

        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret(),
                "--realm", realm.getName());
        assertThat("should fail because admin client doesn't exist in non-master realm",
                result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("invalid_client"));
    }

    @Test
    void testInlineAuthWithClientSecret() {
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret());

        assertThat("inline auth with secret should succeed: " + result.err(),
                result.exitCode(), is(0));
        assertThat(result.out(), containsString("clientId"));
        assertThat("should return a JSON array", result.out().trim(), startsWith("["));
    }

    @Test
    void testInlineAuthWithWrongSecret() {
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--client", Config.getAdminClientId(),
                "--secret", "wrong-secret");

        assertThat("wrong secret should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("unauthorized_client"));
    }

    @Test
    void testInlineAuthWithUserPassword() {
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--user", Config.getAdminUsername(),
                "--password", Config.getAdminPassword());

        assertThat("inline auth with user/password should succeed: " + result.err(),
                result.exitCode(), is(0));
        assertThat(result.out(), containsString("clientId"));
        assertThat("should return a JSON array", result.out().trim(), startsWith("["));
    }

    @Test
    void testInlineAuthWithWrongPassword() {
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--user", Config.getAdminUsername(),
                "--password", "wrong-password");

        assertThat("wrong password should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Invalid user credentials"));
    }

    @Test
    void testInlineAuthWithToken() {
        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--token", getTokenFromConfig());

        assertThat("inline auth with token should succeed: " + result.err(),
                result.exitCode(), is(0));
        assertThat(result.out(), containsString("clientId"));
        assertThat("should return a JSON array", result.out().trim(), startsWith("["));
    }

    @Test
    void testInlineAuthWithWrongToken() {
        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--token", "invalid-token-value");

        assertThat("wrong token should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("401"));
    }

    @Test
    void testInlineTokenWithoutServerFails() {
        // No config, no --server — should fail because server URL is unknown
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--token", getTokenFromConfig());

        assertThat("token without server should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("No server URL configured"));
    }

    @Test
    void testInlineServerOverridesConfig() {
        // Config has a valid server, but we override with a wrong one
        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--server", "http://localhost:1/nonexistent",
                "--token", getTokenFromConfig());

        assertThat("overridden server should be used and fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Connection refused"));
    }

    @Test
    void testInlineUserPasswordOverridesSavedToken() {
        // Config has a valid saved token, but wrong inline credentials should cause failure,
        // proving the inline auth path is taken instead of using the saved token
        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--user", "nonexistent-user",
                "--password", "wrong-password");

        assertThat("inline credentials should override saved token", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Invalid user credentials"));
    }

    @Test
    void testUserWithoutPasswordPromptsInNonInteractive() {
        // In test environment System.console() is null, so prompting fails with a clear message
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--user", Config.getAdminUsername());

        assertThat("should fail when password can't be prompted",
                result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Console is not active"));
    }

    @Test
    void testClientWithoutSecretFails() {
        // --client without --secret or --user doesn't trigger inline auth — server rejects with 401
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--client", Config.getAdminClientId());

        assertThat("should fail without credentials",
                result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("HTTP 401 Unauthorized"));
    }

    @Test
    void testNoConfigAndConfigMutuallyExclusive() {
        CommandResult result = kcAdmV2CmdRaw("client", "list", "-c",
                "--no-config", "--config", "/some/path");

        assertThat("--no-config and --config should be mutually exclusive",
                result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("mutually exclusive"));
    }

    @Test
    void testKeystoreAndSecretMutuallyExclusive() {
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--keystore", "/some/keystore.jks",
                "--secret", "some-secret");

        assertThat("--keystore and --secret should be mutually exclusive",
                result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Can't use both --keystore and --secret"));
    }

    @Test
    void testKeystoreFileNotFound() {
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--keystore", "/nonexistent/keystore.jks",
                "--storepass", "password");

        assertThat("non-existent keystore should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("No such keystore file"));
    }

    @Test
    void testTlsOptionsIgnoredForHttp() {
        // TLS options should be silently ignored for non-HTTPS server URLs
        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--truststore", "/nonexistent/truststore.jks");

        assertThat("should succeed (truststore ignored for HTTP): " + result.err(),
                result.exitCode(), is(0));
        assertThat("should return client data", result.out(), containsString("clientId"));
        assertThat("should return a JSON array", result.out().trim(), startsWith("["));
    }

    @Test
    void testInvalidServerUrl() {
        CommandResult result = kcAdmV2CmdNoConfig("client", "create", "saml",
                "--server", "blab", "--token", "sh");

        assertThat("should fail gracefully", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Invalid server URL"));
    }

    @Test
    void testNoConfigNoServerShowsV2Hint() {
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c");

        assertThat("should fail without server", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("No server URL configured"));
        assertThat("error hint should include --v2",
                result.err(), containsString(KcAdmMain.CMD + " --v2 config credentials"));
    }

    @Test
    void testServerAndRealmWithoutCredentialsFails() {
        // --server and --realm provided but no credentials — server rejects with 401
        CommandResult result = kcAdmV2CmdNoConfig("client", "create", "saml",
                "--server", keycloakUrls.getBase(), "--realm", "master");

        assertThat("should fail without credentials", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("HTTP 401 Unauthorized"));
    }

    @Test
    void testInlineAuthWithTargetRealm() {
        // Auth against master (default), target another realm with -r
        assertThat("managed realm should not be master", realm.getName(), is(not("master")));

        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret(),
                "-r", realm.getName());

        assertThat("should succeed targeting other realm with master auth: " + result.err(),
                result.exitCode(), is(0));
        assertThat("should return a JSON array", result.out().trim(), startsWith("["));
        assertThat("should list clients from target realm, not master",
                result.out(), containsString("/realms/" + realm.getName() + "/"));
    }

    @Test
    void testDefaultRealmIsMaster() {
        // No --realm passed, no config realm — should default to master
        CommandResult result = kcAdmV2CmdNoConfig("client", "list", "-c",
                "--server", keycloakUrls.getBase(),
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret());

        assertThat("should succeed with default master realm: " + result.err(),
                result.exitCode(), is(0));
        assertThat("should list clients from master realm",
                result.out(), containsString("/realms/master/"));
    }

    @Test
    void testRealmOptionOverridesConfigAuthRealm() {
        // Config is for master. --realm non-master with inline credentials
        // should authenticate against the specified realm, not master.
        // The admin client only exists in master, so this should fail.
        assertThat("managed realm should not be master", realm.getName(), is(not("master")));

        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--realm", realm.getName(),
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret());

        assertThat("should fail because admin client doesn't exist in non-master realm",
                result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("invalid_client"));
    }

    @Test
    void testGetClientWithSpecialCharsInId() {
        // ID with URL-special characters — without URL encoding, the request URL would be malformed
        CommandResult result = kcAdmV2Cmd("client", "get", "id with spaces/and#hash");

        assertThat("should fail for non-existent client", result.exitCode(), is(not(0)));
        assertThat("URL-encoded ID should reach the server and get a proper 404, not a malformed URL error",
                result.err(), containsString("Cannot find the specified client"));
    }

    @Test
    void testApplyCreatesNewClient() throws Exception {
        Path jsonFile = new File(tempDir, "put-create.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "put-created", "protocol": "openid-connect", "enabled": true}
                """);

        CommandResult result = kcAdmV2Cmd("client", "apply", "oidc", "put-created", "-f", jsonFile.toString());
        assertThat("PUT create should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("\"clientId\" : \"put-created\""));
        assertThat(result.out(), containsString("\"enabled\" : true"));
    }

    @Test
    void testApplyExistingClient() throws Exception {
        CommandResult createResult = kcAdmV2Cmd("client", "create", "oidc",
                "--client-id", "put-existing", "--enabled", "true", "--description", "original");
        assertThat("setup: create should succeed", createResult.exitCode(), is(0));

        Path jsonFile = new File(tempDir, "put-apply.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "put-existing", "protocol": "openid-connect", "enabled": false, "description": "updated"}
                """);

        CommandResult applyResult = kcAdmV2Cmd("client", "apply", "oidc", "put-existing", "-f", jsonFile.toString());
        assertThat("PUT apply should succeed: " + applyResult.err(), applyResult.exitCode(), is(0));
        assertThat(applyResult.out(), containsString("\"enabled\" : false"));
        assertThat(applyResult.out(), containsString("\"description\" : \"updated\""));
        assertThat("PUT replaces the whole resource", applyResult.out(), containsString("\"clientId\" : \"put-existing\""));
    }

    @Test
    void testApplyWithFieldOptions() {
        kcAdmV2Cmd("client", "create", "oidc", "--client-id", "put-with-options", "--enabled", "true");

        CommandResult result = kcAdmV2Cmd("client", "apply", "oidc", "put-with-options",
                "--client-id", "put-with-options", "--enabled", "false");
        assertThat("PUT with field options should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("\"enabled\" : false"));
        assertThat(result.out(), containsString("\"clientId\" : \"put-with-options\""));
    }

    @Test
    void testApplyWithoutClientIdFails() throws Exception {
        Path jsonFile = new File(tempDir, "put-no-clientid.json").toPath();
        Files.writeString(jsonFile, """
                {"protocol": "openid-connect", "enabled": true}
                """);

        CommandResult result = kcAdmV2Cmd("client", "apply", "oidc", "some-id", "-f", jsonFile.toString());
        assertThat("PUT without clientId should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), is("""
                Provided data is invalid:
                	- clientId: must not be blank
                """));
    }

    @Test
    void testApplyWithMismatchedClientIdFails() throws Exception {
        Path jsonFile = new File(tempDir, "put-mismatch.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "wrong-id", "protocol": "openid-connect"}
                """);

        CommandResult result = kcAdmV2Cmd("client", "apply", "oidc", "correct-id", "-f", jsonFile.toString());
        assertThat("PUT with mismatched clientId should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("does not match"));
    }

    @Test
    void testApplyWithMalformedJsonFile() throws Exception {
        Path jsonFile = new File(tempDir, "put-bad.json").toPath();
        Files.writeString(jsonFile, "not json at all");

        CommandResult result = kcAdmV2Cmd("client", "apply", "oidc", "any-id", "-f", jsonFile.toString());
        assertThat("PUT with malformed JSON should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Cannot parse the JSON"));
    }

    @Test
    void testApplyNonExistentFile() {
        CommandResult result = kcAdmV2Cmd("client", "apply", "oidc", "any-id", "-f", "/nonexistent/file.json");
        assertThat("PUT with missing file should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("File not found"));
    }

    @Test
    void testDeleteClient() throws Exception {
        String id = createClientFromFile("test-for-delete");

        CommandResult getResult = kcAdmV2Cmd("client", "get", id);
        assertThat("get before delete should succeed", getResult.exitCode(), is(0));

        CommandResult deleteResult = kcAdmV2Cmd("client", "delete", id);
        assertThat("delete should succeed: " + deleteResult.err(), deleteResult.exitCode(), is(0));
        assertThat("delete should print confirmation", deleteResult.out(), containsString("Client deleted"));

        getResult = kcAdmV2Cmd("client", "get", id);
        assertThat("get after delete should fail", getResult.exitCode(), is(not(0)));
        assertThat(getResult.err(), containsString("Cannot find the specified client"));
    }

    @Test
    void testLoginAutoFetchFailsGracefully() {
        // This server does NOT have OPENAPI enabled, so auto-fetch on login should fail gracefully
        Path cacheDir = tempDir.toPath().resolve("auto-fetch-fail");
        String autoFetchConfigFile = new File(tempDir, "auto-fetch-fail.config").getAbsolutePath();
        HttpUtil.clearHttpClient();

        CommandResult result = kcAdmV2Cmd(cacheDir, autoFetchConfigFile,
                "config", "credentials",
                "--server", keycloakUrls.getBase(),
                "--realm", "master",
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret());

        assertThat("login should succeed even when auto-fetch fails: " + result.err(), result.exitCode(), is(0));
        assertFalse(Files.exists(cacheDir.resolve(REGISTRY_FILENAME)),
                "no registry should be created when auto-fetch fails");

        // Warning should explain: what failed, why it matters, and how to fix it
        assertThat("should mention fetch failure: " + result.err(),
                result.err(), containsString("Failed to fetch OpenAPI"));
        assertThat("should explain why it matters: " + result.err(),
                result.err(), containsString("CLI commands may not match your server version"));
        assertThat("should suggest manual fallback: " + result.err(),
                result.err(), containsString("config openapi"));
    }

    @Test
    void testClientCreateOidcFromFile() throws Exception {
        Path jsonFile = new File(tempDir, "create-from-file-oidc.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "file-no-disc", "protocol": "openid-connect", "enabled": true}
                """);

        CommandResult result = kcAdmV2Cmd("client", "create", "--file", jsonFile.toString());
        assertThat("'client create --file' should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("file-no-disc"));

        String id = extractId(result);
        CommandResult getResult = kcAdmV2Cmd("client", "get", id);
        assertThat("get should succeed", getResult.exitCode(), is(0));
        assertThat(getResult.out(), containsString("file-no-disc"));
    }

    @Test
    void testClientCreateSamlFromFile() throws Exception {
        Path jsonFile = new File(tempDir, "create-from-file-saml.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "saml-no-disc", "protocol": "saml", "enabled": true}
                """);

        CommandResult result = kcAdmV2Cmd("client", "create", "-f", jsonFile.toString());
        assertThat("'client create -f' for SAML should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("saml-no-disc"));

        String id = extractId(result);
        CommandResult getResult = kcAdmV2Cmd("client", "get", id);
        assertThat("get should succeed", getResult.exitCode(), is(0));
        assertThat(getResult.out(), containsString("saml-no-disc"));
    }

    @Test
    void testClientApplyFromFile() throws Exception {
        CommandResult createResult = kcAdmV2Cmd("client", "create", "oidc",
                "--client-id", "apply-no-disc", "--enabled", "true");
        assertThat("setup: create should succeed", createResult.exitCode(), is(0));

        Path jsonFile = new File(tempDir, "apply-from-file.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "apply-no-disc", "protocol": "openid-connect", "enabled": false}
                """);

        CommandResult result = kcAdmV2Cmd("client", "apply", "-f", jsonFile.toString());
        assertThat("'client apply -f' should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("\"enabled\" : false"));
    }

    @Test
    void testClientPatchFromFile() throws Exception {
        CommandResult createResult = kcAdmV2Cmd("client", "create", "oidc",
                "--client-id", "patch-no-disc", "--enabled", "true");
        assertThat("setup: create should succeed", createResult.exitCode(), is(0));
        String id = extractId(createResult);

        Path jsonFile = new File(tempDir, "patch-from-file.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "%s", "enabled": false}
                """.formatted(id));

        CommandResult result = kcAdmV2Cmd("client", "patch", "-f", jsonFile.toString());
        assertThat("'client patch -f' should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("\"enabled\" : false"));
    }

    @Test
    void testClientCreateWithoutFileOrSubcommandFails() {
        CommandResult result = kcAdmV2Cmd("client", "create");
        assertThat("'client create' without file or subcommand should fail", result.exitCode(), is(not(0)));
        assertThat("should tell user to provide a file: " + result.err(), result.err(), containsString("-f/--file"));
    }

    @Test
    void testClientCreateFileNotFound() {
        CommandResult result = kcAdmV2Cmd("client", "create", "-f", "/nonexistent/file.json");
        assertThat("should fail for non-existent file", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("not found"));
    }

    @Test
    void testClientPatchFromFileMissingClientId() throws Exception {
        Path jsonFile = new File(tempDir, "patch-no-id.json").toPath();
        Files.writeString(jsonFile, """
                {"enabled": false}
                """);

        CommandResult result = kcAdmV2Cmd("client", "patch", "-f", jsonFile.toString());
        assertThat("'client patch -f' without clientId in file should fail", result.exitCode(), is(not(0)));
        assertThat("should report the missing field", result.err(), containsString("does not contain required"));
        assertThat("should name the expected field", result.err(), containsString("clientId"));
    }

    @Test
    void testClientPatchFromFileBlankClientId() throws Exception {
        Path jsonFile = new File(tempDir, "patch-blank-id.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "", "enabled": false}
                """);

        CommandResult result = kcAdmV2Cmd("client", "patch", "-f", jsonFile.toString());
        assertThat("'client patch -f' with blank clientId should fail, err: " + result.err(), result.exitCode(), is(not(0)));
        assertThat("should report the missing field, err: " + result.err(), result.err(),
                containsString("does not contain required"));
        assertThat("should name the expected field, err: " + result.err(), result.err(), containsString("clientId"));
    }

    @Test
    void testClientPatchFromFileMalformedJson() throws Exception {
        Path jsonFile = new File(tempDir, "patch-bad.json").toPath();
        Files.writeString(jsonFile, "{ not valid json }");

        CommandResult result = kcAdmV2Cmd("client", "patch", "-f", jsonFile.toString());
        assertThat("'client patch -f' with malformed JSON should fail, err: " + result.err(),
                result.exitCode(), is(not(0)));
        assertThat("should report local JSON parsing failure from ID extraction, err: " + result.err(),
                result.err(), containsString("Cannot parse JSON to extract ID"));
    }

    @Test
    void testClientCreateFromFileBeforeSubcommand() throws Exception {
        Path jsonFile = new File(tempDir, "create-before-sub.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "create-before-sub", "protocol": "openid-connect", "enabled": true}
                """);

        // -f before the variant subcommand: parent consumes -f, leaf must pick it up
        CommandResult result = kcAdmV2Cmd("client", "create", "-f", jsonFile.toString(), "oidc");
        assertThat("-f before subcommand should not be silently ignored, err: " + result.err()
                        + ", out: " + result.out(),
                result.exitCode(), is(0));
        assertThat("file content should be used as request body, out: " + result.out(),
                result.out(), containsString("create-before-sub"));
    }

    @Test
    void testClientPatchFromFileBeforeSubcommand() throws Exception {
        CommandResult createResult = kcAdmV2Cmd("client", "create", "saml",
                "--client-id", "patch-before-sub", "--enabled", "true");
        assertThat("setup: create should succeed", createResult.exitCode(), is(0));
        String id = extractId(createResult);

        Path jsonFile = new File(tempDir, "patch-before-sub.json").toPath();
        Files.writeString(jsonFile, """
                {"enabled": false}
                """);

        // -f before the variant subcommand: parent consumes -f, leaf must pick it up
        CommandResult result = kcAdmV2Cmd("client", "patch", "-f", jsonFile.toString(),
                "saml", id);
        assertThat("-f before subcommand should not be silently ignored, err: " + result.err()
                        + ", out: " + result.out(),
                result.exitCode(), is(0));
        assertThat("file content should be applied as patch body, out: " + result.out(),
                result.out(), containsString("\"enabled\" : false"));
    }

    @Test
    void testFileBeforeSubcommandAndFieldOptionsMutuallyExclusive() {
        // -f on parent + field options on leaf should still be rejected as mutually exclusive
        // no real file needed — mutual exclusivity check fires before file access
        CommandResult result = kcAdmV2Cmd("client", "apply", "-f", "/any/path.json",
                "oidc", "exclusive-test", "--client-id", "exclusive-test");
        assertThat("-f before subcommand with field options should fail, err: " + result.err()
                        + ", out: " + result.out(),
                result.exitCode(), is(not(0)));
        assertThat("should report mutual exclusivity, err: " + result.err(),
                result.err(), containsString("mutually exclusive"));
    }

    @Test
    void testFileSpecifiedOnBothParentAndSubcommandFails() {
        // no real files needed — duplicate check fires before file access
        CommandResult result = kcAdmV2Cmd("client", "create",
                "-f", "/first.json", "oidc", "-f", "/second.json");
        assertThat("-f on both parent and subcommand should fail, err: " + result.err()
                        + ", out: " + result.out(),
                result.exitCode(), is(not(0)));
        assertThat("should report duplicate -f, err: " + result.err(),
                result.err(), containsString("-f/--file"));
    }

    private String createClientWithAllParams(String clientId) {
        CommandResult result = kcAdmV2Cmd("client", "create", "oidc",
                "--client-id", clientId,
                "--display-name", "Test Full Client",
                "--description", "A test client with all params",
                "--enabled", "true",
                "--app-url", "https://example.com",
                "--redirect-uris", "https://example.com/callback,https://example.com/logout",
                "--roles", "role1,role2",
                "--login-flows", "STANDARD,SERVICE_ACCOUNT",
                "--auth-method", "client_secret");
        assertThat("create should succeed: " + result.err(), result.exitCode(), is(0));
        return extractId(result);
    }

    private String createClientFromFile(String clientId) throws Exception {
        Path jsonFile = new File(tempDir, clientId + ".json").toPath();
        Files.writeString(jsonFile, """
                {
                    "clientId": "%s",
                    "protocol": "saml",
                    "enabled": true
                }
                """.formatted(clientId));
        CommandResult result = kcAdmV2Cmd("client", "create", "saml", "-f", jsonFile.toString());
        assertThat("create from file should succeed: " + result.err(), result.exitCode(), is(0));
        return extractId(result);
    }

    private CommandResult kcAdmV2Cmd(String... args) {
        return kcAdmV2Cmd(null, configFilePath, args);
    }

    private String getTokenFromConfig() {
        try {
            setConfigFile(configFilePath);
            var handler = new FileConfigHandler();
            var config = handler.loadConfig();
            return config.sessionRealmConfigData().getToken();
        } catch (Exception e) {
            throw new AssertionError("Could not read token from config", e);
        }
    }

    private String extractId(CommandResult result) {
        try {
            return new ObjectMapper().readTree(result.out()).get("clientId").asText();
        } catch (Exception e) {
            throw new AssertionError("Could not extract clientId from: " + result.out(), e);
        }
    }

    public static class V2ApiServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
