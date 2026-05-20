package org.keycloak.client.admin.cli.commands.v2;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.v2.KcAdmV2Cmd;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.OptionDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.ResourceDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2Completer;
import org.keycloak.client.admin.cli.v2.KcAdmV2DescriptorCache;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.util.ConfigUtil;
import org.keycloak.util.JsonSerialization;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KcAdmV2CachedDescriptorHelpTest {

    private static final String TEST_SERVER = "http://test-server:8080";
    private static final String CONFIG_FILE_NAME = Path.of(KcAdmMain.DEFAULT_CONFIG_FILE_PATH).getFileName().toString();

    private static Path tempDir;

    @BeforeClass
    public static void setUpClass() throws IOException {
        tempDir = Files.createTempDirectory("kcadm-help-test");
        setUpCachedDescriptor();
        ConfigUtil.setHandler(null);
        FileConfigHandler.setConfigFile(null);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            deleteRecursively(tempDir);
        }
    }

    @After
    public void tearDown() {
        ConfigUtil.setHandler(null);
        FileConfigHandler.setConfigFile(null);
    }

    @Test
    public void helpShowsCachedResource() {
        String help = createCli().getUsageMessage();
        assertTrue("'widget' not found in: " + help, help.contains("widget"));
        assertFalse("bundled 'client' should not appear in: " + help, help.contains("client"));
    }

    @Test
    public void helpShowsCachedSubcommands() {
        CommandLine widgetCli = createCli().getSubcommands().get("widget");
        assertNotNull("'widget' should be a subcommand", widgetCli);

        String help = widgetCli.getUsageMessage();
        assertTrue("'list' not found in: " + help, help.contains("list"));
        assertTrue("'create' not found in: " + help, help.contains("create"));
    }

    @Test
    public void helpShowsCachedOptions() {
        CommandLine createCli = createCli().getSubcommands().get("widget").getSubcommands().get("create");
        String help = createCli.getUsageMessage();
        assertTrue("'--widget-name' not found in: " + help, help.contains("--widget-name"));
        assertTrue("'Name of the widget' not found in: " + help, help.contains("Name of the widget"));
    }

    @Test
    public void autocompleteShowsCachedResourceAndNotBundled() {
        List<String> candidates = complete("");
        assertTrue("'widget' not found in: " + candidates, candidates.contains("widget"));
        assertFalse("bundled 'client' should not appear in: " + candidates, candidates.contains("client"));
    }

    @Test
    public void autocompleteShowsCachedSubcommands() {
        List<String> candidates = complete("widget", "");
        assertTrue("'list' not found in: " + candidates, candidates.contains("list"));
        assertTrue("'create' not found in: " + candidates, candidates.contains("create"));
    }

    @Test
    public void autocompleteShowsCachedOptions() {
        List<String> candidates = complete("widget", "create", "--");
        assertTrue("'--widget-name' not found in: " + candidates, candidates.contains("--widget-name"));
    }

    @Test
    public void fallsBackToBundledWhenCacheHasNoEntryForServer() throws IOException {
        Path emptyCache = Files.createTempDirectory("kcadm-empty-cache");
        try {
            CommandLine cli = createCliWithCacheDir(emptyCache, "http://unknown-server:8080");
            String help = cli.getUsageMessage();
            assertTrue("should fall back to bundled 'client': " + help, help.contains("client"));
            assertFalse("cached 'widget' should not appear: " + help, help.contains("widget"));
        } finally {
            deleteRecursively(emptyCache);
        }
    }

    @Test
    public void fallsBackToBundledWhenConfigHasNoServerUrl() throws IOException {
        Path emptyCache = Files.createTempDirectory("kcadm-empty-cache");
        try {
            CommandLine cli = createCliWithCacheDir(emptyCache, null);
            String help = cli.getUsageMessage();
            assertTrue("should fall back to bundled 'client': " + help, help.contains("client"));
        } finally {
            deleteRecursively(emptyCache);
        }
    }

    private static void setUpCachedDescriptor() throws IOException {
        KcAdmV2DescriptorCache cache = new KcAdmV2DescriptorCache(tempDir);
        cache.save(TEST_SERVER, widgetDescriptor());

        Path configFile = tempDir.resolve(CONFIG_FILE_NAME);
        ConfigData config = new ConfigData();
        config.setServerUrl(TEST_SERVER);
        Files.writeString(configFile, JsonSerialization.writeValueAsPrettyString(config));
    }

    private CommandLine createCli() {
        return createCliWithCacheDir(tempDir, TEST_SERVER);
    }

    private static CommandLine createCliWithCacheDir(Path cacheDir, String serverUrl) {
        try {
            Path configFile = cacheDir.resolve(CONFIG_FILE_NAME);
            ConfigData config = new ConfigData();
            if (serverUrl != null) {
                config.setServerUrl(serverUrl);
            }
            Files.writeString(configFile, JsonSerialization.writeValueAsPrettyString(config));
            FileConfigHandler.setConfigFile(configFile.toString());
            ConfigUtil.setHandler(new FileConfigHandler());
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up config", e);
        }

        return Globals.createCommandLine(new KcAdmV2Cmd(cacheDir), KcAdmMain.CMD,
                new PrintWriter(System.err, true));
    }

    private List<String> complete(String... args) {
        StringWriter sw = new StringWriter();
        KcAdmV2Completer.complete(args, new PrintWriter(sw), tempDir);
        String output = sw.toString().trim();
        if (output.isEmpty()) {
            return List.of();
        }
        return List.of(output.split(System.lineSeparator()));
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        }
    }

    private static KcAdmV2CommandDescriptor widgetDescriptor() {
        OptionDescriptor opt = new OptionDescriptor();
        opt.setName("widget-name");
        opt.setFieldName("widgetName");
        opt.setType(OptionDescriptor.TYPE_STRING);
        opt.setDescription("Name of the widget");

        CommandDescriptor listCmd = new CommandDescriptor();
        listCmd.setName("list");
        listCmd.setResourceName("widget");
        listCmd.setHttpMethod("GET");
        listCmd.setPath("/admin/api/{realmName}/widgets/{version}");
        listCmd.setDescription("List widgets");
        listCmd.setOptions(List.of());

        CommandDescriptor createCmd = new CommandDescriptor();
        createCmd.setName("create");
        createCmd.setResourceName("widget");
        createCmd.setHttpMethod("POST");
        createCmd.setPath("/admin/api/{realmName}/widgets/{version}");
        createCmd.setDescription("Create a widget");
        createCmd.setOptions(List.of(opt));

        ResourceDescriptor resource = new ResourceDescriptor();
        resource.setName("widget");
        resource.setCommands(List.of(listCmd, createCmd));

        KcAdmV2CommandDescriptor descriptor = new KcAdmV2CommandDescriptor();
        descriptor.setVersion("99.0.0");
        descriptor.setResources(List.of(resource));
        return descriptor;
    }
}
