package org.keycloak.tests.admin.cli.v2;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.v2.KcAdmV2Cmd;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.keycloak.client.cli.config.FileConfigHandler.setConfigFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@KeycloakIntegrationTest(config = KcAdmV2ClientCLITest.V2ApiServerConfig.class)
public class KcAdmV2ClientCLITest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

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
        assertThat("error should clarify that provided data are invalid: " + result.err(),
                result.err().toLowerCase(), containsString("invalid"));
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
        assertThat(result.err(), containsString("Could not find client"));
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
                result.err(), containsString("kcadm.sh --v2 config credentials"));
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
                result.err(), containsString("Could not find client"));
    }

    @Test
    void testUpdateCreatesNewClient() throws Exception {
        Path jsonFile = new File(tempDir, "put-create.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "put-created", "protocol": "openid-connect", "enabled": true}
                """);

        CommandResult result = kcAdmV2Cmd("client", "update", "oidc", "put-created", "-f", jsonFile.toString());
        assertThat("PUT create should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("\"clientId\" : \"put-created\""));
        assertThat(result.out(), containsString("\"enabled\" : true"));
    }

    @Test
    void testUpdateExistingClient() throws Exception {
        CommandResult createResult = kcAdmV2Cmd("client", "create", "oidc",
                "--client-id", "put-existing", "--enabled", "true", "--description", "original");
        assertThat("setup: create should succeed", createResult.exitCode(), is(0));

        Path jsonFile = new File(tempDir, "put-update.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "put-existing", "protocol": "openid-connect", "enabled": false, "description": "updated"}
                """);

        CommandResult updateResult = kcAdmV2Cmd("client", "update", "oidc", "put-existing", "-f", jsonFile.toString());
        assertThat("PUT update should succeed: " + updateResult.err(), updateResult.exitCode(), is(0));
        assertThat(updateResult.out(), containsString("\"enabled\" : false"));
        assertThat(updateResult.out(), containsString("\"description\" : \"updated\""));
        assertThat("PUT replaces the whole resource", updateResult.out(), containsString("\"clientId\" : \"put-existing\""));
    }

    @Test
    void testUpdateWithFieldOptions() {
        kcAdmV2Cmd("client", "create", "oidc", "--client-id", "put-with-options", "--enabled", "true");

        CommandResult result = kcAdmV2Cmd("client", "update", "oidc", "put-with-options",
                "--client-id", "put-with-options", "--enabled", "false");
        assertThat("PUT with field options should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("\"enabled\" : false"));
        assertThat(result.out(), containsString("\"clientId\" : \"put-with-options\""));
    }

    @Test
    void testUpdateWithoutClientIdFails() throws Exception {
        Path jsonFile = new File(tempDir, "put-no-clientid.json").toPath();
        Files.writeString(jsonFile, """
                {"protocol": "openid-connect", "enabled": true}
                """);

        CommandResult result = kcAdmV2Cmd("client", "update", "oidc", "some-id", "-f", jsonFile.toString());
        assertThat("PUT without clientId should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("does not match"));
    }

    @Test
    void testUpdateWithMismatchedClientIdFails() throws Exception {
        Path jsonFile = new File(tempDir, "put-mismatch.json").toPath();
        Files.writeString(jsonFile, """
                {"clientId": "wrong-id", "protocol": "openid-connect"}
                """);

        CommandResult result = kcAdmV2Cmd("client", "update", "oidc", "correct-id", "-f", jsonFile.toString());
        assertThat("PUT with mismatched clientId should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("does not match"));
    }

    @Test
    void testUpdateWithMalformedJsonFile() throws Exception {
        Path jsonFile = new File(tempDir, "put-bad.json").toPath();
        Files.writeString(jsonFile, "not json at all");

        CommandResult result = kcAdmV2Cmd("client", "update", "oidc", "any-id", "-f", jsonFile.toString());
        assertThat("PUT with malformed JSON should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Cannot parse the JSON"));
    }

    @Test
    void testUpdateNonExistentFile() {
        CommandResult result = kcAdmV2Cmd("client", "update", "oidc", "any-id", "-f", "/nonexistent/file.json");
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
        assertThat(getResult.err(), containsString("Could not find client"));
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
        CommandLine cli = Globals.createCommandLine(new KcAdmV2Cmd(), KcAdmMain.CMD, new PrintWriter(System.err, true));

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        String[] fullArgs = new String[args.length + 2];
        System.arraycopy(args, 0, fullArgs, 0, args.length);
        fullArgs[args.length] = "--config";
        fullArgs[args.length + 1] = configFilePath;

        int exitCode = cli.execute(fullArgs);
        return new CommandResult(exitCode, out.toString(), err.toString());
    }

    private CommandResult kcAdmV2CmdRaw(String... args) {
        CommandLine cli = Globals.createCommandLine(new KcAdmV2Cmd(), KcAdmMain.CMD, new PrintWriter(System.err, true));

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute(args);
        return new CommandResult(exitCode, out.toString(), err.toString());
    }

    private CommandResult kcAdmV2CmdNoConfig(String... args) {
        CommandLine cli = Globals.createCommandLine(new KcAdmV2Cmd(), KcAdmMain.CMD, new PrintWriter(System.err, true));

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        String[] fullArgs = new String[args.length + 1];
        System.arraycopy(args, 0, fullArgs, 0, args.length);
        fullArgs[args.length] = "--no-config";

        int exitCode = cli.execute(fullArgs);
        return new CommandResult(exitCode, out.toString(), err.toString());
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

    record CommandResult(int exitCode, String out, String err) {
    }

    public static class V2ApiServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
