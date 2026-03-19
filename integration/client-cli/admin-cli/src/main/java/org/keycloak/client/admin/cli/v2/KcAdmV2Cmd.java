package org.keycloak.client.admin.cli.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.keycloak.client.admin.cli.commands.ConfigCmd;
import org.keycloak.client.cli.common.BaseGlobalOptionsCmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import static org.keycloak.client.admin.cli.KcAdmMain.CMD;
import static org.keycloak.client.admin.cli.KcAdmMain.V2_FLAG;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;

@Command(name = "kcadm",
        description = "%nCOMMAND [ARGUMENTS]"
)
public class KcAdmV2Cmd extends BaseGlobalOptionsCmd {

    private static final String BUNDLED_DESCRIPTOR = "/kcadm-v2-commands.json";

    @Spec
    CommandSpec spec;

    @Override
    protected boolean nothingToDo() {
        return true;
    }

    @Override
    protected String help() {
        return "";
    }

    @Override
    protected void printHelpIfNeeded() {
        PrintWriter out = new PrintWriter(System.out, true);
        out.println("Keycloak Admin CLI v2 (experimental)");
        out.println();
        out.println("Use '" + CMD + " " + V2_FLAG + " config credentials' to start a session.");
        out.println();
        out.println("For example:");
        out.println();
        out.println("  " + PROMPT + " " + CMD + " " + V2_FLAG + " config credentials --server http://localhost:8080 --realm master --user admin");
        out.println();
        spec.commandLine().usage(out);
        out.println();
        out.println("Use '" + CMD + " " + V2_FLAG + " <command> --help' for more information about a command.");
        out.println("Find more information at: https://www.keycloak.org/docs/latest");
        System.exit(CommandLine.ExitCode.OK);
    }

    @Override
    protected void configureCommandLine(CommandLine cli) {
        cli.getCommandSpec().name(CMD + " " + V2_FLAG);
        CommandLine configCmd = new CommandLine(new ConfigCmd());
        configCmd.getCommandSpec().usageMessage().description("Configuration management");
        cli.addSubcommand(configCmd);
        KcAdmV2CommandDescriptor descriptor = loadDescriptor();
        KcAdmV2CommandBuilder.addCommands(cli, descriptor);
    }

    private KcAdmV2CommandDescriptor loadDescriptor() {
        // TODO: fetch and cache server-specific descriptor (follow-up PR)
        return loadBundledDescriptor();
    }

    private KcAdmV2CommandDescriptor loadBundledDescriptor() {
        try (InputStream is = getClass().getResourceAsStream(BUNDLED_DESCRIPTOR)) {
            if (is == null) {
                throw new RuntimeException("Bundled command descriptor not found: " + BUNDLED_DESCRIPTOR);
            }
            return KcAdmV2DescriptorBuilder.readDescriptor(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load command descriptor", e);
        }
    }
}
