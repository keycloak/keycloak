package org.keycloak.client.admin.cli.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.cli.common.BaseAuthOptionsCmd;
import org.keycloak.client.cli.util.Headers;
import org.keycloak.client.cli.util.HeadersBody;
import org.keycloak.client.cli.util.HeadersBodyStatus;
import org.keycloak.client.cli.util.HttpUtil;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "openapi", description = "Fetch the server's OpenAPI spec to get commands, options, and help " +
        "that match your server version — instead of the built-in defaults.%n%n" +
        "The source can be a URL (e.g., https://localhost:9000/openapi) or a local file path.%n%n" +
        "The OpenAPI endpoint is served on the management interface, which may use a different host and port " +
        "than the main server. Requires 'config credentials' to be run first.")
class KcAdmV2ConfigOpenApiCmd implements Runnable {

    @Parameters(index = "0", paramLabel = "<source>",
            description = "URL of the OpenAPI endpoint (e.g., http://localhost:9000/openapi) or path to a local OpenAPI JSON file")
    String openApiSource;

    @Option(names = "--config", description = "Path to the config file (${sys:" + BaseAuthOptionsCmd.DEFAULT_CONFIG_PATH_STRING_KEY + "} by default)")
    String config;

    @Option(names = { "-h", "--help" }, usageHelp = true, hidden = true)
    boolean help;

    @Spec
    CommandSpec spec;

    private final Path cacheDir;

    KcAdmV2ConfigOpenApiCmd(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    public void run() {
        String configPath = config != null ? config : KcAdmMain.DEFAULT_CONFIG_FILE_PATH;
        String serverUrl = KcAdmV2Cmd.readServerUrlFrom(configPath);
        if (serverUrl == null) {
            throw new RuntimeException("No server configured. Run 'config credentials' first.");
        }

        try {
            KcAdmV2CommandDescriptor descriptor = loadDescriptor(openApiSource);
            KcAdmV2DescriptorCache cache = new KcAdmV2DescriptorCache(cacheDir);
            cache.save(serverUrl, descriptor);
            spec.commandLine().getErr().println(
                    "OpenAPI descriptor cached for " + serverUrl + " (version " + descriptor.getVersion() + ")");
        } catch (Exception e) {
            String cause = e.getMessage() != null ? ": " + e.getMessage() : "";
            throw new RuntimeException("Failed to load OpenAPI from " + openApiSource + cause, e);
        }
    }

    static KcAdmV2CommandDescriptor loadDescriptor(String source) throws IOException {
        KcAdmV2CommandDescriptor descriptor;
        if (isUrl(source)) {
            descriptor = fetchDescriptorFromUrl(source);
        } else {
            File file = new File(source);
            if (!file.isFile()) {
                throw new RuntimeException("Not a valid URL (must start with http:// or https://) and no file found at: " + source);
            }
            descriptor = loadDescriptorFromFile(file);
        }

        if (descriptor.getResources() == null || descriptor.getResources().isEmpty()) {
            throw new RuntimeException("OpenAPI spec contains no resources — the file may not be a valid OpenAPI spec");
        }

        return descriptor;
    }

    private static boolean isUrl(String source) {
        return source.startsWith("http://") || source.startsWith("https://");
    }

    static KcAdmV2CommandDescriptor fetchDescriptorFromUrl(String url) throws IOException {
        HeadersBodyStatus response = HttpUtil.doRequest("get", url, new HeadersBody(new Headers()));
        response.checkSuccess();
        InputStream body = response.getBody();
        OpenAPI openApi = KcAdmV2DescriptorBuilder.parseOpenApi(() -> body);
        return KcAdmV2DescriptorBuilder.convert(openApi);
    }

    private static KcAdmV2CommandDescriptor loadDescriptorFromFile(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            OpenAPI openApi = KcAdmV2DescriptorBuilder.parseOpenApi(() -> is);
            return KcAdmV2DescriptorBuilder.convert(openApi);
        }
    }
}
