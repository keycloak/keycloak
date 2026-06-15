package org.keycloak.client.admin.cli.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.commands.ConfigCmd;
import org.keycloak.client.cli.common.BaseGlobalOptionsCmd;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.util.JsonSerialization;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import static org.keycloak.client.admin.cli.KcAdmMain.CMD;
import static org.keycloak.client.admin.cli.KcAdmMain.V2_FLAG;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;

@Command(name = "kcadm",
        description = "%nCOMMAND [ARGUMENTS]",
        footer = {"%nEnable tab completion:%n  source <(kcadm.sh --v2 completion)"}
)
public class KcAdmV2Cmd extends BaseGlobalOptionsCmd {

    private static final String BUNDLED_DESCRIPTOR = "/kcadm-v2-commands.json";
    private static final String CONFIG_FILE_NAME = Path.of(KcAdmMain.DEFAULT_CONFIG_FILE_PATH).getFileName().toString();
    private static final String CONFIG_OPTION = "--config";

    private static final Path DEFAULT_CACHE_DIR =
            Path.of(KcAdmMain.DEFAULT_CONFIG_FILE_PATH).getParent().resolve("command-descriptors").resolve("v2");

    private final Path cacheDir;
    private final String configFilePath;

    @Spec
    CommandSpec spec;

    public KcAdmV2Cmd() {
        this(DEFAULT_CACHE_DIR);
    }

    public KcAdmV2Cmd(Path cacheDir) {
        this(cacheDir, null);
    }

    public KcAdmV2Cmd(String[] args) {
        this(DEFAULT_CACHE_DIR, args);
    }

    public KcAdmV2Cmd(Path cacheDir, String[] args) {
        this.cacheDir = cacheDir;
        this.configFilePath = findConfigPath(args);
    }

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
        CommandLine configCmd = new CommandLine(new ConfigCmd(true));
        configCmd.getCommandSpec().usageMessage().description("Configuration management");
        configCmd.getCommandSpec().removeSubcommand("credentials");
        configCmd.addSubcommand("credentials", new CommandLine(new KcAdmV2ConfigCredentialsCmd(cacheDir)));
        configCmd.addSubcommand("openapi", new CommandLine(new KcAdmV2ConfigOpenApiCmd(cacheDir)));
        configCmd.addSubcommand("editor", new CommandLine(new KcAdmV2ConfigEditorCmd()));
        cli.addSubcommand(configCmd);
        KcAdmV2CommandDescriptor descriptor = loadDescriptor();
        KcAdmV2CommandBuilder.addCommands(cli, descriptor);
    }

    private KcAdmV2CommandDescriptor loadDescriptor() {
        KcAdmV2DescriptorCache cache = new KcAdmV2DescriptorCache(cacheDir);

        String serverUrl = readServerUrlFromConfig();
        if (serverUrl != null) {
            KcAdmV2CommandDescriptor cached = cache.loadForServer(serverUrl);
            if (cached != null) {
                return cached;
            }
        }

        return loadBundledDescriptor();
    }

    private String readServerUrlFromConfig() {
        if (configFilePath != null) {
            String fromConfig = readServerUrlFrom(configFilePath);
            if (fromConfig != null) {
                return fromConfig;
            }
        }
        String fromCacheDir = readServerUrlFrom(cacheDir.resolve(CONFIG_FILE_NAME).toString());
        if (fromCacheDir != null) {
            return fromCacheDir;
        }
        return readServerUrlFrom(KcAdmMain.DEFAULT_CONFIG_FILE_PATH);
    }

    static String readServerUrlFrom(String configFilePath) {
        try {
            File configFile = new File(configFilePath);
            if (!configFile.isFile()) {
                return null;
            }
            try (FileInputStream is = new FileInputStream(configFile)) {
                ConfigData config = JsonSerialization.readValue(is, ConfigData.class);
                return config.getServerUrl();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String findConfigPath(String[] args) {
        if (args != null) {
            for (int i = 0; i < args.length - 1; i++) {
                if (CONFIG_OPTION.equals(args[i])) {
                    return args[i + 1];
                }
            }
        }
        return null;
    }

    public static KcAdmV2CommandDescriptor loadBundledDescriptor() {
        try (InputStream is = KcAdmV2Cmd.class.getResourceAsStream(BUNDLED_DESCRIPTOR)) {
            if (is == null) {
                throw new RuntimeException("Bundled command descriptor not found: " + BUNDLED_DESCRIPTOR);
            }
            return KcAdmV2DescriptorBuilder.readDescriptor(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load command descriptor", e);
        }
    }
}
