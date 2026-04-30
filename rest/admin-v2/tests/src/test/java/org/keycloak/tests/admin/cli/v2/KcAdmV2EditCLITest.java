package org.keycloak.tests.admin.cli.v2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = KcAdmV2EditCLITest.V2ApiServerConfig.class)
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Test scripts require POSIX shell and Unix utilities")
public class KcAdmV2EditCLITest extends AbstractKcAdmV2CLITest {

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
    void testEditNoChanges() {
        createClient("edit-no-changes");
        setEditor("/usr/bin/true");

        CommandResult result = kcAdmV2Cmd("client", "edit", "edit-no-changes");

        assertThat("edit with no changes should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat("should report no changes", result.err(), containsString("no changes"));
        assertThat("should not print any resource to stdout", result.out().trim(), is(""));
    }

    @Test
    void testEditAppliesUpdate() throws Exception {
        CommandResult createResult = createClient("edit-applies-update", "--enabled", "true");
        assertThat("client should be created as enabled",
                createResult.out(), containsString("\"enabled\" : true"));
        setEditor(createEnabledFlipEditor());

        CommandResult result = kcAdmV2Cmd("client", "edit", "edit-applies-update");

        assertThat("edit should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat("updated resource should reflect the change",
                result.out(), containsString("\"enabled\" : false"));
        assertThat("updated resource should contain the clientId",
                result.out(), containsString("\"clientId\" : \"edit-applies-update\""));

        CommandResult getResult = kcAdmV2Cmd("client", "get", "edit-applies-update");
        assertThat("get after edit should succeed", getResult.exitCode(), is(0));
        assertThat("change should be persisted", getResult.out(), containsString("\"enabled\" : false"));
    }

    @Test
    void testEditNonExistentResource() {
        setEditor("/usr/bin/true");

        CommandResult result = kcAdmV2Cmd("client", "edit", "non-existent-id");

        assertThat("edit non-existent should fail", result.exitCode(), is(not(0)));
        assertThat(result.err().trim(), is("Could not find client"));
    }

    @Test
    void testEditEditorExitsNonZero() {
        createClient("edit-non-zero");
        setEditor("/usr/bin/false");

        CommandResult result = kcAdmV2Cmd("client", "edit", "edit-non-zero");

        assertThat("edit should fail when editor exits non-zero", result.exitCode(), is(not(0)));
        assertThat("should explain the editor failed",
                result.err(), containsString("exited with error"));
        assertThat("should tell user how to reconfigure the editor",
                result.err(), containsString("config editor"));
    }

    @Test
    void testEditEditorNotFound() {
        createClient("edit-not-found");
        setEditor(tempDir.toPath().resolve("nonexistent-editor").toString());

        CommandResult result = kcAdmV2Cmd("client", "edit", "edit-not-found");

        assertThat("edit should fail when editor not found", result.exitCode(), is(not(0)));
        assertThat("should tell user how to configure via command",
                result.err(), containsString("config editor"));
        assertThat("should tell user about the env var alternative",
                result.err(), containsString("KC_CLI_EDITOR"));
    }

    @Test
    void testEditWithRealmOverride() throws Exception {
        assertThat("managed realm should not be master", realm.getName(), is(not("master")));

        CommandResult createResult = kcAdmV2Cmd("client", "create", "oidc",
                "-r", realm.getName(),
                "--client-id", "edit-realm-override",
                "--enabled", "true");
        assertThat("setup: create should succeed: " + createResult.err(), createResult.exitCode(), is(0));
        assertThat("client should be created as enabled",
                createResult.out(), containsString("\"enabled\" : true"));

        setEditor(createEnabledFlipEditor());

        CommandResult result = kcAdmV2Cmd("client", "edit", "edit-realm-override", "-r", realm.getName());

        assertThat("edit in other realm should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat(result.out(), containsString("\"enabled\" : false"));
    }

    @Test
    void testEditInvalidJsonAfterEdit() throws Exception {
        createClient("edit-invalid-json");
        setEditor(createTestEditor("""
                #!/bin/sh
                echo "this is not json" > "$1"
                """));

        CommandResult result = kcAdmV2Cmd("client", "edit", "edit-invalid-json");

        assertThat("edit with invalid JSON should fail", result.exitCode(), is(not(0)));
        assertThat("should report JSON error", result.err(), containsString("not valid JSON"));
    }

    private CommandResult createClient(String clientId, String... extraArgs) {
        final String[] baseArgs = {"client", "create", "oidc", "--client-id", clientId};
        final String[] args;
        if (extraArgs.length == 0) {
            args = baseArgs;
        } else {
            args = new String[baseArgs.length + extraArgs.length];
            System.arraycopy(baseArgs, 0, args, 0, baseArgs.length);
            System.arraycopy(extraArgs, 0, args, baseArgs.length, extraArgs.length);
        }

        CommandResult result = kcAdmV2Cmd(args);
        assertThat("setup: create should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat("create should return the resource",
                result.out(), containsString("\"clientId\" : \"" + clientId + "\""));
        return result;
    }

    private void setEditor(String editor) {
        CommandResult result = kcAdmV2Cmd("config", "editor", editor);
        assertThat("config editor should succeed: " + result.err(), result.exitCode(), is(0));
        assertThat("should confirm editor configuration: " + result.err(), result.err(), containsString(editor));
        try {
            assertThat("editor should be persisted in config", Files.readString(Path.of(configFilePath)), containsString(editor));
        } catch (Exception e) {
            fail("Could not read config file: " + configFilePath, e);
        }
    }

    private String createEnabledFlipEditor() throws Exception {
        return createTestEditor("""
                #!/bin/sh
                sed 's/"enabled" : true/"enabled" : false/' "$1" > "$1.tmp" && mv "$1.tmp" "$1"
                """);
    }

    private String createTestEditor(String script) throws Exception {
        Path editorScript = Files.createTempFile(tempDir.toPath(), "test-editor-", ".sh");
        Files.writeString(editorScript, script);
        assertThat("editor script should be made executable", editorScript.toFile().setExecutable(true), is(true));
        return editorScript.toString();
    }

    private CommandResult kcAdmV2Cmd(String... args) {
        return kcAdmV2Cmd(null, configFilePath, args);
    }

    public static class V2ApiServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
