package org.keycloak.client.admin.cli.commands.v2;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.commands.ConfigCmd;
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
        assertFalse("should not have --sign-documents", help.contains("--sign-documents"));
        assertFalse("create should not expose readOnly --uuid: " + help, help.contains("--uuid"));
        assertFalse("variant leaf should not show -f (use parent instead): " + help, help.contains(" -f"));
        assertFalse("variant leaf should not show --file (use parent instead): " + help, help.contains("--file"));
        assertFalse("variant leaf should not show --client-id (use positional): " + help, help.contains("--client-id"));
        assertTrue("variant leaf should show <id> positional but found:" + help, help.contains("<id>"));
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
        assertTrue("--login-flows paramLabel should hint 'SERVICE_ACCOUNT' in pipe-separated format: " + help,
                help.contains("SERVICE_ACCOUNT|") || help.contains("|SERVICE_ACCOUNT"));
        assertTrue("array enum paramLabel should use <...>[,...] format: " + help,
                help.contains("<") && help.contains(">[,...]"));
    }

    @Test
    public void testCreateSamlShowsSamlOptions() {
        String help = getVariantHelp("create", "saml");
        assertTrue("should have --sign-documents", help.contains("--sign-documents"));
        assertTrue("should have --sign-assertions", help.contains("--sign-assertions"));
        assertTrue("should have --name-id-format", help.contains("--name-id-format"));
        assertFalse("should not have --login-flows", help.contains("--login-flows"));
        assertFalse("variant leaf should not show -f (use parent instead): " + help, help.contains(" -f"));
        assertFalse("variant leaf should not show --file (use parent instead): " + help, help.contains("--file"));
        assertFalse("variant leaf should not show --client-id (use positional): " + help, help.contains("--client-id"));
        assertTrue("variant leaf should show <id> positional but found:" + help, help.contains("<id>"));
        assertTrue("--name-id-format paramLabel should hint 'username' in pipe-separated format: " + help,
                help.contains("|username") || help.contains("username|"));
        assertTrue("--signature-algorithm paramLabel should hint 'RSA_SHA512' in pipe-separated format: " + help,
                help.contains("|RSA_SHA512") || help.contains("RSA_SHA512|"));
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
        String help = out.toString();
        assertFalse("apply variant should not show --client-id (use positional): " + help, help.contains("--client-id"));
        assertTrue("apply variant should show <id> positional but found: " + help, help.contains("<id>"));
        assertFalse("apply variant should not show -f (use parent instead): " + help, help.contains(" -f"));
        assertFalse("apply variant should not show --file (use parent instead): " + help, help.contains("--file"));
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
    public void testLeafDoesNotHaveConnectionOptions() {
        String help = getSubcommandHelp("client", "list");
        assertFalse("leaf should not have 'Connection options:' heading", help.contains("Connection options:"));
        assertFalse("leaf should not have --config", help.contains("--config"));
        assertFalse("leaf should not have -r", help.contains("-r,"));
        assertFalse("leaf should not have --target-realm", help.contains("--target-realm"));
        assertFalse("leaf should not have --token", help.contains("--token"));
        assertFalse("leaf should not have --insecure", help.contains("--insecure"));
    }

    @Test
    public void testLeafDoesNotHaveKeystoreOptions() {
        String help = getSubcommandHelp("client", "list");
        assertFalse("leaf should not have --keystore", help.contains("--keystore"));
        assertFalse("leaf should not have --storepass", help.contains("--storepass"));
        assertFalse("leaf should not have --keypass", help.contains("--keypass"));
        assertFalse("leaf should not have --alias", help.contains("--alias"));
        assertFalse("leaf should not mention KC_CLI_STORE_PASSWORD", help.contains("KC_CLI_STORE_PASSWORD"));
    }

    @Test
    public void testLeafDoesNotHaveConfigOptions() {
        String help = getSubcommandHelp("client", "list");
        assertFalse("leaf should not have --no-config", help.contains("--no-config"));
        assertFalse("leaf should not describe no-config purpose", help.contains("Don't use config file"));
    }

    @Test
    public void testRootHelpShowsConnectionOptions() {
        String help = createCli().getUsageMessage();
        assertTrue("root should have 'Connection options:' but found: " + help, help.contains("Connection options:"));
        assertTrue("root should have --server but found: " + help, help.contains("--server"));
        assertTrue("root should have --realm but found: " + help, help.contains("--realm"));
        assertTrue("root should have --config but found: " + help, help.contains("--config"));
        assertTrue("root should have --token but found: " + help, help.contains("--token"));
        assertTrue("root should have --user but found: " + help, help.contains("--user"));
        assertTrue("root should have --password but found: " + help, help.contains("--password"));
        assertTrue("root should have --no-config but found: " + help, help.contains("--no-config"));
        assertTrue("root should describe no-config purpose but found: " + help, help.contains("Don't use config file"));
        assertTrue("root should have --target-realm but found: " + help, help.contains("--target-realm"));
        assertTrue("root should have --truststore but found: " + help, help.contains("--truststore"));
        assertTrue("root should have --trustpass but found: " + help, help.contains("--trustpass"));
        assertTrue("root should have --insecure but found: " + help, help.contains("--insecure"));
        assertTrue("root should have --keystore but found: " + help, help.contains("--keystore"));
        assertTrue("root should have --storepass but found: " + help, help.contains("--storepass"));
        assertTrue("root should have --keypass but found: " + help, help.contains("--keypass"));
        assertTrue("root should have --alias but found: " + help, help.contains("--alias"));
        assertTrue("--password should mention KC_CLI_PASSWORD but found: " + help, help.contains("KC_CLI_PASSWORD"));
        assertTrue("--secret should mention KC_CLI_CLIENT_SECRET but found: " + help, help.contains("KC_CLI_CLIENT_SECRET"));
        assertFalse("root should not show --help as a connection option but found: " + help,
                help.contains("Print command specific help"));
        assertTrue("--storepass should mention KC_CLI_STORE_PASSWORD but found: " + help, help.contains("KC_CLI_STORE_PASSWORD"));
    }

    @Test
    public void testGroupCommandShowsSubcommands() {
        CommandLine cli = createCli();
        String help = cli.getSubcommands().get("client").getUsageMessage();
        assertTrue("should list subcommands like 'list'", help.contains("list"));
        assertTrue("should list subcommands like 'create'", help.contains("create"));
    }

    @Test
    public void testLeafSynopsisShowsConnectionOptionsBeforeCommand() {
        String help = getSubcommandHelp("client", "list");
        String prefix = KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG + " [CONNECTION OPTIONS] client list";
        assertTrue("leaf synopsis should contain: " + prefix + ", got: " + help, help.contains(prefix));
    }

    @Test
    public void testVariantSynopsisShowsConnectionOptionsBeforeCommand() {
        String help = getVariantHelp("create", "oidc");
        String prefix = KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG + " [CONNECTION OPTIONS] client create oidc";
        assertTrue("variant synopsis should contain: " + prefix + ", got: " + help, help.contains(prefix));
    }

    @Test
    public void testVariantParentSynopsisShowsMutuallyExclusiveOptions() {
        String help = getVariantParentHelp("create");
        String expected = KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG
                + " [CONNECTION OPTIONS] client create [-f <file> | oidc | saml]";
        assertTrue("variant parent synopsis should show: " + expected + ", got: " + help,
                help.contains(expected));
    }

    @Test
    public void testGroupSynopsisShowsConnectionOptionsBeforeCommand() {
        CommandLine cli = createCli();
        String help = cli.getSubcommands().get("client").getUsageMessage();
        String prefix = KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG + " [CONNECTION OPTIONS] client";
        assertTrue("group synopsis should contain: " + prefix + ", got: " + help, help.contains(prefix));
    }

    @Test
    public void testVariantDoesNotHaveConnectionOptions() {
        String help = getVariantHelp("create", "oidc");
        assertFalse("variant should not have --config", help.contains("--config"));
        assertFalse("variant should not have --realm", help.contains("--realm"));
        assertFalse("variant should not have --server", help.contains("--server"));
    }

    @Test
    public void testCreateVariantFailsWhenNoIdProvided() {
        CommandLine cli = createCli();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("--no-config", "--server", "http://localhost:8080", "--token", "fake",
                "client", "create", "saml");
        assertNotEquals("create saml with no ID and no file should fail", 0, exitCode);
        assertTrue("should fail because no identifier was provided but found: " + err, err.toString().contains("<id>"));
    }

    @Test
    public void testCreateVariantFailsWhenPositionalAndOptionIdDiffer() {
        CommandLine cli = createCli();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("--no-config", "--server", "http://localhost:8080", "--token", "fake",
                "client", "create", "oidc", "positional-id", "--client-id", "different-id");
        assertNotEquals("positional and --client-id with different values should fail", 0, exitCode);
        assertTrue("error should mention positional value but found: " + err, err.toString().contains("positional-id"));
        assertTrue("error should mention --client-id option name but found: " + err, err.toString().contains("--client-id"));
        assertTrue("error should mention --client-id value but found: " + err, err.toString().contains("different-id"));
    }

    @Test
    public void testCreateVariantAcceptsSameIdFromPositionalAndOption() {
        CommandLine cli = createCli();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("--no-config", "client", "create", "oidc",
                "same-id", "--client-id", "same-id");
        assertNotEquals("should fail due to --no-config (no server), not due to ID conflict", 0, exitCode);
        assertTrue("same values should pass ID validation, should fail for No server but found: " + err,
                err.toString().contains("No server"));
    }

    @Test
    public void testPositionalIdAndFileMutuallyExclusive() {
        CommandLine cli = createCli();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("--no-config", "--server", "http://localhost:8080", "--token", "fake",
                "client", "create", "oidc", "my-client", "-f", "/any/file.json");
        assertNotEquals("positional <id> and -f should not be used together", 0, exitCode);
        assertTrue("should mention <id> and -f mutually exclusive but found: " + err,
                err.toString().contains("<id>") && err.toString().contains("mutually exclusive"));
    }

    @Test
    public void testFileOptionRejectsNonExistentFile() {
        CommandLine cli = createCli();
        cli.setErr(new PrintWriter(new StringWriter()));
        int exitCode = cli.execute("client", "create", "oidc", "-f", "/nonexistent/file.json");
        assertNotEquals("Should fail for non-existent file", 0, exitCode);
    }

    // technically not a help test, but it tests that the help suggestion works
    @Test
    public void testConfigEditorUsesRootConfigOption() throws Exception {
        File configFile = File.createTempFile("kcadm-test", ".config");
        configFile.deleteOnExit();
        Files.writeString(configFile.toPath(), "{}");

        CommandLine cli = createCli();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(new StringWriter()));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute("--config", configFile.getAbsolutePath(),
                "config", "editor", "nano");
        assertEquals("config editor with --config on root should succeed: " + err, 0, exitCode);

        String saved = Files.readString(configFile.toPath());
        assertTrue("config file should contain editor setting but found: " + saved,
                saved.contains("nano"));
    }

    @Test
    public void testErrorMessageIncludesV2Flag() {
        CommandLine cli = createCli();
        StringWriter err = new StringWriter();
        cli.setErr(new PrintWriter(err));
        cli.execute("client", "patch", "unknown-variant");
        String error = err.toString();
        assertTrue("error hint should include --v2: " + error,
                error.contains(KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG) && error.contains("client patch --help"));
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
            assertFalse("--help should not show connection options", out.toString().contains("--config"));
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
    public void testHelpBeforeSubcommandShowsClientHelp() {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(new StringWriter()));

        // showHelpForLeafCommand moves --help to the end, same as KcAdmMain.main() does
        // manually applied here to avoid System.exit
        String[] args = {"--help", "client"};
        KcAdmMain.showHelpForLeafCommand(args);
        int exitCode = cli.execute(args);
        assertEquals("--help client should exit with 0", 0, exitCode);
        String output = out.toString();
        assertTrue("should show client help with subcommands listed, got: " + output,
                output.contains("list") && output.contains("create"));
    }

    @Test
    public void testHelpOnConfigBeforeSubcommandShowsEditorHelp() {
        CommandLine cli = createCli();
        StringWriter out = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(new StringWriter()));

        // showHelpForLeafCommand moves --help to the end, same as KcAdmMain.main() does
        // manually applied here to avoid System.exit
        String[] args = {"config", "--help", "editor"};
        KcAdmMain.showHelpForLeafCommand(args);
        int exitCode = cli.execute(args);
        assertEquals("config --help editor should exit with 0", 0, exitCode);
        String output = out.toString();
        assertTrue("should show editor help with <editor> parameter, got: " + output,
                output.contains("<editor>") && output.contains("--config"));
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
    public void testVariantParentDoesNotShowConnectionOptions() {
        String help = getVariantParentHelp("create");
        assertFalse("create parent should not show --config: " + help, help.contains("--config"));
        assertFalse("create parent should not show --server: " + help, help.contains("--server"));
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
        String help = out.toString();
        assertFalse("patch variant should not show --client-id (use positional): " + help, help.contains("--client-id"));
        assertTrue("patch variant should show <id> positional but found: " + help, help.contains("<id>"));
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
    public void testEditDoesNotHaveConnectionOptions() {
        String help = getSubcommandHelp("client", "edit");
        assertFalse("edit should not have --config", help.contains("--config"));
        assertFalse("edit should not have --realm", help.contains("--realm"));
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
        String expectedPrefix = KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG + " config editor";
        assertTrue("synopsis should contain: " + expectedPrefix + ", got: " + help, help.contains(expectedPrefix));
        assertFalse("config editor should not show --realm: " + help, help.contains("--realm"));
    }

    @Test
    public void testListShowsQueryParameterOptions() {
        String help = getSubcommandHelp("client", "list");
        assertTrue("list should show --fields option but found: " + help, help.contains("--fields"));
        assertTrue("list should show --q option but found: " + help, help.contains("--q"));
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
    public void testDumpTraceOptionAcceptedOnRoot() {
        assertDumpTraceAccepted("-x", "--no-config", "client", "list");
    }

    @Test
    public void testDumpTraceOptionAcceptedOnLeaf() {
        assertDumpTraceAccepted("--no-config", "client", "list", "-x");
    }

    private void assertDumpTraceAccepted(String... args) {
        Globals.dumpTrace = false;
        java.io.PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(OutputStream.nullOutputStream()));

            CommandLine cli = createCli();
            StringWriter err = new StringWriter();
            StringWriter out = new StringWriter();
            cli.setErr(new PrintWriter(err));
            cli.setOut(new PrintWriter(out));

            cli.execute(args);
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
        String expectedPrefix = KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG + " config openapi";
        assertTrue("synopsis should contain: " + expectedPrefix + ", got: " + output, output.contains(expectedPrefix));
        assertFalse("config openapi should not show --server: " + output, output.contains("--server"));
    }

    @Test
    public void testConfigCredentialsHelpShowsOpenApiUrl() {
        CommandLine cli = createCli();
        String help = ((BaseConfigCredentialsCmd) cli.getSubcommands().get("config")
                .getSubcommands().get("credentials").getCommand()).help();
        assertTrue("should show --openapi-url option: " + help, help.contains("--openapi-url"));
        assertTrue("should show --v2 in command: " + help, help.contains("--v2"));
    }

    @Test
    public void testConfigHelpShowsAllV2Subcommands() {
        CommandLine cli = createCli();
        String help = ((ConfigCmd) cli.getSubcommands().get("config").getCommand()).help();
        assertTrue("should list 'editor': " + help, help.contains("editor"));
        assertTrue("should list 'openapi': " + help, help.contains("openapi"));
        assertTrue("should list 'credentials': " + help, help.contains("credentials"));
        assertTrue("should list 'truststore': " + help, help.contains("truststore"));
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
