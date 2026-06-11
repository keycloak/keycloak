package org.keycloak.client.admin.cli.commands.v2;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.v2.KcAdmV2Cmd;
import org.keycloak.client.cli.common.BaseConfigCredentialsCmd;
import org.keycloak.client.cli.common.Globals;

import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class KcAdmV2HelpTest {

    @Test
    public void testHelpShowsResourceGroup() {
        String help = createCli().getUsageMessage();

        assertTrue("Help should list 'client' resource group", help.contains("client"));
        assertTrue("Help should list 'config' command", help.contains("config"));
    }

    @Test
    public void testHelpShowsCompletionInstructions() {
        String help = createCli().getUsageMessage();
        assertTrue("Help should mention tab completion setup", help.contains("completion"));
    }

    @Test
    public void testHelpShowsConsistentDescriptions() {
        String help = createCli().getUsageMessage();

        assertTrue("config should have a proper description",
                help.contains("Configuration management"));
        assertTrue("client should have a proper description",
                help.contains("Client operations"));
    }

    @Test
    public void testGroupCommandWithoutSubcommand() {
        CommandLine cli = createCli();
        StringWriter err = new StringWriter();
        cli.setErr(new PrintWriter(err));
        int exitCode = cli.execute("client");
        assertEquals("should exit normally", 0, exitCode);
        assertTrue("should suggest full command with --v2",
                err.toString().contains(KcAdmMain.CMD + " --v2 client --help"));
    }

    @Test
    public void testClientHelpShowsAllCommands() {
        CommandLine cli = createCli();
        CommandLine clientCli = cli.getSubcommands().get("client");

        String help = clientCli.getUsageMessage();
        for (String cmd : List.of("list", "create", "get", "patch", "apply", "delete", "edit")) {
            assertTrue("Client help should list '" + cmd + "'", help.contains(cmd));
        }
    }

    @Test
    public void testCreateHasProtocolVariants() {
        CommandLine cli = createCli();
        CommandLine createCli = cli.getSubcommands().get("client").getSubcommands().get("create");
        assertTrue("create should have 'oidc' subcommand", createCli.getSubcommands().containsKey("oidc"));
        assertTrue("create should have 'saml' subcommand", createCli.getSubcommands().containsKey("saml"));
    }

    @Test
    public void testCreateOidcShowsOidcOptions() {
        String help = getVariantHelp("create", "oidc");
        assertTrue("should have --login-flows", help.contains("--login-flows"));
        assertTrue("should have --web-origins", help.contains("--web-origins"));
        assertTrue("should have --service-account-roles", help.contains("--service-account-roles"));
        assertTrue("should have -f", help.contains("-f"));
        assertFalse("should not have --sign-documents", help.contains("--sign-documents"));
        assertFalse("create should not expose readOnly --uuid: " + help, help.contains("--uuid"));
    }

    @Test
    public void testAuthFlattenedToSubOptions() {
        String help = getVariantHelp("create", "oidc");
        assertTrue("should have --auth-method", help.contains("--auth-method"));
        assertTrue("should have --auth-secret", help.contains("--auth-secret"));
        assertTrue("should have --auth-certificate", help.contains("--auth-certificate"));
        assertFalse("should not have bare --auth ", help.contains("  --auth "));
    }

    @Test
    public void testLoginFlowsShowsEnumValues() {
        String help = getVariantHelp("create", "oidc");
        assertTrue("should show STANDARD", help.contains("STANDARD"));
        assertTrue("should show SERVICE_ACCOUNT", help.contains("SERVICE_ACCOUNT"));
        assertTrue("should show DIRECT_GRANT", help.contains("DIRECT_GRANT"));
    }

    @Test
    public void testCreateSamlShowsSamlOptions() {
        String help = getVariantHelp("create", "saml");
        assertTrue("should have --sign-documents", help.contains("--sign-documents"));
        assertTrue("should have --sign-assertions", help.contains("--sign-assertions"));
        assertTrue("should have --name-id-format", help.contains("--name-id-format"));
        assertTrue("should have -f", help.contains("-f"));
        assertFalse("should not have --login-flows", help.contains("--login-flows"));
    }

    @Test
    public void testPatchOidcShowsOidcOptions() {
        String help = getVariantHelp("patch", "oidc");
        assertTrue("should have --login-flows", help.contains("--login-flows"));
        assertTrue("should have -f", help.contains("-f"));
        assertFalse("should not have --sign-documents", help.contains("--sign-documents"));
        assertFalse("patch should not expose readOnly --uuid: " + help, help.contains("--uuid"));
    }

    @Test
    public void testPatchSamlShowsSamlOptions() {
        String help = getVariantHelp("patch", "saml");
        assertTrue("should have --sign-documents", help.contains("--sign-documents"));
        assertFalse("should not have --login-flows", help.contains("--login-flows"));
    }

    @Test
    public void testApplyHasProtocolVariants() {
        CommandLine cli = createCli();
        CommandLine applyCli = cli.getSubcommands().get("client").getSubcommands().get("apply");
        assertTrue("apply should have 'oidc' subcommand", applyCli.getSubcommands().containsKey("oidc"));
        assertTrue("apply should have 'saml' subcommand", applyCli.getSubcommands().containsKey("saml"));
    }

    @Test
    public void testApplyOidcShowsOidcOptions() {
        String help = getVariantHelp("apply", "oidc");
        assertTrue("should have --login-flows", help.contains("--login-flows"));
        assertTrue("should have -f", help.contains("-f"));
        assertTrue("should have <id> positional", help.contains("<id>"));
        assertFalse("should not have --sign-documents", help.contains("--sign-documents"));
        assertFalse("apply should not expose readOnly --uuid: " + help, help.contains("--uuid"));
    }

    @Test
    public void testApplySamlShowsSamlOptions() {
        String help = getVariantHelp("apply", "saml");
        assertTrue("should have --sign-documents", help.contains("--sign-documents"));
        assertTrue("should have <id> positional", help.contains("<id>"));
        assertFalse("should not have --login-flows", help.contains("--login-flows"));
    }

    @Test
    public void testApplyHasOutputOptions() {
        String help = getVariantHelp("apply", "oidc");
        assertTrue("apply (200 response) should have Output options", help.contains("Output options:"));
        assertTrue("should have --compressed", help.contains("--compressed"));
    }

    @Test
    public void testHelpOnApplyVariantLeafWithRequiredId() {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("client", "apply", "oidc", "--help");
        assertEquals("--help on apply variant should exit with 0, err: " + err, 0, exitCode);
        assertTrue("should show help with --client-id option", out.toString().contains("--client-id"));
    }

    @Test
    public void testFileOptionNotAvailableOnGet() {
        String help = getSubcommandHelp("client", "get");
        assertFalse("get should not have --file option", help.contains("--file"));
    }

    @Test
    public void testFileOptionNotAvailableOnList() {
        String help = getSubcommandHelp("client", "list");
        assertFalse("list should not have --file option", help.contains("--file"));
    }

    @Test
    public void testFileOptionNotAvailableOnDelete() {
        String help = getSubcommandHelp("client", "delete");
        assertFalse("delete should not have --file option", help.contains("--file"));
    }

    @Test
    public void testOutputOptionsNotAvailableOnDelete() {
        String help = getSubcommandHelp("client", "delete");
        assertFalse("delete (204 No Content) should not have --compressed", help.contains("--compressed"));
        assertFalse("delete (204 No Content) should not have 'Output options:'", help.contains("Output options:"));
    }

    @Test
    public void testOutputOptionsGroupedOnSubcommand() {
        String help = getSubcommandHelp("client", "list");
        assertTrue("should have 'Output options:' heading", help.contains("Output options:"));
        assertTrue("should have -c", help.contains("-c"));
        assertTrue("should have --compressed", help.contains("--compressed"));
    }

    @Test
    public void testOutputOptionsAvailableOnVariant() {
        String help = getVariantHelp("create", "oidc");
        assertTrue("should have -c", help.contains("-c"));
    }

    @Test
    public void testConnectionOptionsOnSubcommand() {
        String help = getSubcommandHelp("client", "list");
        assertTrue("should have 'Connection options:' heading", help.contains("Connection options:"));
        assertTrue("should have --config", help.contains("--config"));
        assertTrue("should have -r", help.contains("-r"));
        assertTrue("should have --target-realm", help.contains("--target-realm"));
        assertTrue("should have --realm", help.contains("--realm"));
        assertTrue("should have --token", help.contains("--token"));
        assertTrue("should have --truststore", help.contains("--truststore"));
        assertTrue("should have --trustpass", help.contains("--trustpass"));
        assertTrue("should have --insecure", help.contains("--insecure"));
    }

    @Test
    public void testKeystoreOptionsAvailable() {
        String help = getSubcommandHelp("client", "list");
        assertTrue("should have --keystore", help.contains("--keystore"));
        assertTrue("should have --storepass", help.contains("--storepass"));
        assertTrue("should have --keypass", help.contains("--keypass"));
        assertTrue("should have --alias", help.contains("--alias"));
        assertTrue("--storepass should mention KC_CLI_STORE_PASSWORD", help.contains("KC_CLI_STORE_PASSWORD"));
    }

    @Test
    public void testNoConfigOptionAvailable() {
        String help = getSubcommandHelp("client", "list");
        assertTrue("should have --no-config", help.contains("--no-config"));
        assertTrue("should describe no-config purpose", help.contains("Don't use config file"));
    }

    @Test
    public void testPasswordDescriptionMentionsEnvVar() {
        String help = getSubcommandHelp("client", "list");
        assertTrue("--password should mention KC_CLI_PASSWORD", help.contains("KC_CLI_PASSWORD"));
    }

    @Test
    public void testSecretDescriptionMentionsEnvVar() {
        String help = getSubcommandHelp("client", "list");
        assertTrue("--secret should mention KC_CLI_CLIENT_SECRET", help.contains("KC_CLI_CLIENT_SECRET"));
    }

    @Test
    public void testGroupCommandShowsSubcommands() {
        CommandLine cli = createCli();
        String help = cli.getSubcommands().get("client").getUsageMessage();
        assertTrue("should list subcommands like 'list'", help.contains("list"));
        assertTrue("should list subcommands like 'create'", help.contains("create"));
    }

    @Test
    public void testConnectionOptionsAvailableOnVariant() {
        String help = getVariantHelp("create", "oidc");
        assertTrue("should have --config", help.contains("--config"));
        assertTrue("should have --realm", help.contains("--realm"));
    }

    @Test
    public void testFileOptionRejectsNonExistentFile() {
        CommandLine cli = createCli();
        cli.setErr(new PrintWriter(new StringWriter()));
        int exitCode = cli.execute("client", "create", "oidc", "-f", "/nonexistent/file.json");
        assertNotEquals("Should fail for non-existent file", 0, exitCode);
    }

    @Test
    public void testErrorMessageIncludesV2Flag() {
        CommandLine cli = createCli();
        StringWriter err = new StringWriter();
        cli.setErr(new PrintWriter(err));
        cli.execute("client", "patch", "unknown-variant");
        assertTrue("error hint should include --v2",
                err.toString().contains(KcAdmMain.CMD + " --v2 client patch --help"));
    }


    @Test
    public void testHelpFlagOnLeafCommand() {
        Globals.help = false;
        try {
            CommandLine cli = createCli();
            StringWriter out = new StringWriter();
            StringWriter err = new StringWriter();
            cli.setOut(new PrintWriter(out));
            cli.setErr(new PrintWriter(err));

            int exitCode = cli.execute("client", "list", "--help");
            assertEquals("--help should exit with 0", 0, exitCode);
            assertTrue("--help should show connection options", out.toString().contains("--config"));
            assertTrue("--help should show output options", out.toString().contains("--compressed"));
        } finally {
            Globals.help = false;
        }
    }

    @Test
    public void testHelpFlagOnGroupCommand() {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("client", "--help");
        assertEquals("--help on group command should exit with 0", 0, exitCode);
        assertTrue("should show subcommands", out.toString().contains("list"));
    }

    @Test
    public void testVariantParentShowsFileOptionForCreate() {
        String help = getVariantParentHelp("create");
        assertTrue("create parent should show -f option: " + help, help.contains("-f"));
        assertTrue("create parent should show --file option: " + help, help.contains("--file"));
    }

    @Test
    public void testVariantParentShowsFileOptionForApply() {
        String help = getVariantParentHelp("apply");
        assertTrue("apply parent should show -f option: " + help, help.contains("-f"));
        assertTrue("apply parent should show --file option: " + help, help.contains("--file"));
        assertFalse("apply parent should not show <id>: " + help, help.contains("<id>"));
    }

    @Test
    public void testVariantParentShowsFileOptionForPatch() {
        String help = getVariantParentHelp("patch");
        assertTrue("patch parent should show -f option: " + help, help.contains("-f"));
        assertTrue("patch parent should show --file option: " + help, help.contains("--file"));
        assertFalse("patch parent should not show <id>: " + help, help.contains("<id>"));
    }

    @Test
    public void testVariantParentDoesNotShowFieldOptions() {
        String help = getVariantParentHelp("create");
        assertFalse("create parent should not show --client-id: " + help, help.contains("--client-id"));
        assertFalse("create parent should not show --login-flows: " + help, help.contains("--login-flows"));
        assertFalse("create parent should not show --sign-documents: " + help, help.contains("--sign-documents"));
    }

    @Test
    public void testVariantParentShowsConnectionOptions() {
        String help = getVariantParentHelp("create");
        assertTrue("create parent should show --config: " + help, help.contains("--config"));
        assertTrue("create parent should show --server: " + help, help.contains("--server"));
    }

    @Test
    public void testHelpFlagOnVariantParent() {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("client", "create", "--help");
        assertEquals("--help on variant parent should exit with 0", 0, exitCode);
        String help = out.toString();
        assertTrue("should show oidc variant", help.contains("oidc"));
        assertTrue("should show saml variant", help.contains("saml"));
    }

    @Test
    public void testHelpOnVariantLeafWithRequiredId() {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("client", "patch", "oidc", "--help");
        assertEquals("--help on variant leaf with required ID should exit with 0, err: " + err, 0, exitCode);
        assertTrue("should show help with --client-id option", out.toString().contains("--client-id"));
    }

    @Test
    public void testEditHelpMentionsEditorConfiguration() {
        String help = getSubcommandHelp("client", "edit");
        assertTrue("edit help should mention KC_CLI_EDITOR env var", help.contains("KC_CLI_EDITOR"));
        assertTrue("edit help should mention config editor command", help.contains("config editor"));
        assertTrue("edit help should show <id> parameter", help.contains("<id>"));
    }

    @Test
    public void testEditHasNoFileOrFieldOptions() {
        String help = getSubcommandHelp("client", "edit");
        assertFalse("edit should not have --file option", help.contains("--file"));
        assertFalse("edit should not have -f option", help.contains("-f"));
        assertFalse("edit should not have --client-id option", help.contains("--client-id"));
    }

    @Test
    public void testEditHasConnectionAndOutputOptions() {
        String help = getSubcommandHelp("client", "edit");
        assertTrue("edit should have --config", help.contains("--config"));
        assertTrue("edit should have --realm", help.contains("--realm"));
        assertTrue("edit should have Output options", help.contains("Output options:"));
    }

    @Test
    public void testConfigEditorHelpShowsUsage() {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(new StringWriter()));

        int exitCode = cli.execute("config", "editor", "--help");
        assertEquals("--help on config editor should exit with 0", 0, exitCode);
        String help = out.toString();
        assertTrue("should show editor parameter: " + help, help.contains("<editor>"));
        assertTrue("should show --config option: " + help, help.contains("--config"));
    }

    @Test
    public void testAdminRootNotAvailableOnLeafCommand() {
        String help = getSubcommandHelp("client", "list");
        assertFalse("v2 should not have --admin-root", help.contains("--admin-root"));
        assertFalse("v2 should not have -a option", help.contains("-a,"));
    }

    @Test
    public void testGroupCommandHelpOmitsConnectionOptions() {
        CommandLine cli = createCli();
        String help = cli.getSubcommands().get("client").getUsageMessage();
        assertFalse("group command should not show --config", help.contains("--config"));
        assertFalse("group command should not show --server", help.contains("--server"));
        assertFalse("group command should not show --password", help.contains("--password"));
    }

    @Test
    public void testDumpTraceOptionAccepted() {
        Globals.dumpTrace = false;
        java.io.PrintStream originalErr = System.err;
        try {
            // limit noise
            System.setErr(new PrintStream(OutputStream.nullOutputStream()));

            CommandLine cli = createCli();
            StringWriter err = new StringWriter();
            StringWriter out = new StringWriter();
            cli.setErr(new PrintWriter(err));
            cli.setOut(new PrintWriter(out));

            cli.execute("client", "list", "-x", "--no-config");
            // Will fail (no server) but -x should be accepted, not rejected as unknown
            assertFalse("-x should not be reported as unknown option",
                    err.toString().contains("Unknown option"));
            assertTrue("should fail with server error, not option error",
                    err.toString().contains("No server URL configured"));
        } finally {
            System.setErr(originalErr);
            Globals.dumpTrace = false;
        }
    }

    @Test
    public void testConfigOpenApiHelpShowsUrl() {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("config", "openapi", "--help");
        assertEquals("--help on config openapi should exit with 0", 0, exitCode);
        String output = out.toString();
        assertTrue("should show <source> parameter: " + output, output.contains("<source>"));
        assertTrue("should show --config option: " + output, output.contains("--config"));
        assertTrue("should mention 'config credentials': " + output, output.contains("config credentials"));
    }

    @Test
    public void testConfigCredentialsHelpShowsOpenApiUrl() {
        CommandLine cli = createCli();
        String help = ((BaseConfigCredentialsCmd) cli.getSubcommands().get("config")
                .getSubcommands().get("credentials").getCommand()).help();
        assertTrue("should show --openapi-url option: " + help, help.contains("--openapi-url"));
        assertTrue("should show --v2 in command: " + help, help.contains("--v2"));
    }

    private String getVariantParentHelp(String command) {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(new StringWriter()));
        int exitCode = cli.execute("client", command, "--help");
        assertEquals("--help should exit with 0", 0, exitCode);
        return out.toString();
    }

    private String getVariantHelp(String command, String variant) {
        CommandLine cli = createCli();
        return cli.getSubcommands().get("client").getSubcommands().get(command)
                .getSubcommands().get(variant).getUsageMessage();
    }

    private String getSubcommandHelp(String group, String command) {
        CommandLine cli = createCli();
        return cli.getSubcommands().get(group)
                .getSubcommands().get(command)
                .getUsageMessage();
    }

    private CommandLine createCli() {
        return Globals.createCommandLine(new KcAdmV2Cmd(), KcAdmMain.CMD, new PrintWriter(System.err, true));
    }
}
