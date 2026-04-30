package org.keycloak.client.admin.cli.v2;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.cli.common.BaseAuthOptionsCmd;
import org.keycloak.client.cli.config.FileConfigHandler;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;


@Command(name = "editor", description = "Configure the text editor for the edit command")
class KcAdmV2ConfigEditorCmd implements Runnable {

    @Parameters(index = "0", paramLabel = "<editor>",
            description = "Editor command (e.g., vi, nano, 'code --wait')")
    String editorValue;

    @Option(names = "--config", description = "Path to the config file (${sys:" + BaseAuthOptionsCmd.DEFAULT_CONFIG_PATH_STRING_KEY + "} by default)")
    String config;

    @Option(names = {"-h", "--help"}, usageHelp = true, hidden = true)
    boolean help;

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        FileConfigHandler.setConfigFile(config != null ? config : KcAdmMain.DEFAULT_CONFIG_FILE_PATH);
        try {
            new FileConfigHandler().saveMergeConfig(c -> c.setEditor(editorValue));
        } finally {
            // TODO: this must be dropped when we move away from the static config file pattern
            FileConfigHandler.setConfigFile(null);
        }

        spec.commandLine().getErr().println("Editor configured: " + editorValue);
    }

}
