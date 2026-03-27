package org.keycloak.client.admin.cli.v2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.commands.AbstractTargetAuthOptionsCmd;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.OptionDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.VariantDescriptor;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.config.InMemoryConfigHandler;
import org.keycloak.client.cli.util.ConfigUtil;
import org.keycloak.client.cli.util.Headers;
import org.keycloak.client.cli.util.HeadersBody;
import org.keycloak.client.cli.util.HeadersBodyStatus;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.client.cli.util.OutputUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpHeaders;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.cli.util.HttpUtil.APPLICATION_JSON;
import static org.keycloak.client.cli.util.IoUtil.readFully;
import static org.keycloak.common.util.ObjectUtil.capitalize;

@Command
class KcAdmV2RequestExecutor extends AbstractTargetAuthOptionsCmd {

    static final String MERGE_PATCH_JSON = "application/merge-patch+json";
    static final String API_VERSION = "v2";
    static final String DEFAULT_REALM = "master";

    @Spec CommandSpec spec;
    private final CommandDescriptor descriptor;
    private final VariantDescriptor variant;

    KcAdmV2RequestExecutor(CommandDescriptor descriptor, VariantDescriptor variant) {
        super();
        this.descriptor = descriptor;
        this.variant = variant;
    }

    @Override
    protected String help() {
        return "";
    }

    @Override
    protected void processOptions() {
        if (config != null && noconfig) {
            throw new IllegalArgumentException("Options --config and --no-config are mutually exclusive");
        }

        if (noconfig) {
            InMemoryConfigHandler handler = new InMemoryConfigHandler();
            handler.setConfigData(new ConfigData());
            ConfigUtil.setHandler(handler);
        } else {
            FileConfigHandler.setConfigFile(config != null ? config : getDefaultConfigFilePath());
            ConfigUtil.setHandler(new FileConfigHandler());
        }
    }

    @Override
    public void run() {
        if (Globals.help) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return;
        }

        PrintWriter out = spec.commandLine().getOut();

        try {
            processOptions();

            ConfigData configData = loadConfig();

            // Apply CLI overrides onto config
            if (server != null) {
                configData.setServerUrl(server);
            }
            if (realm != null) {
                configData.setRealm(realm);
            }
            if (externalToken != null) {
                configData.setExternalToken(externalToken);
            }

            // Default realm to master if not set anywhere
            if (configData.getRealm() == null) {
                configData.setRealm(DEFAULT_REALM);
            }

            String v2Cmd = KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG;

            if (configData.getServerUrl() == null) {
                throw new RuntimeException(
                        "No server URL configured. Use --server or '" + v2Cmd + " config credentials' first.");
            }
            if (!configData.getServerUrl().startsWith("http://") && !configData.getServerUrl().startsWith("https://")) {
                throw new RuntimeException(
                        "Invalid server URL: " + configData.getServerUrl() + ". URL must start with http:// or https://");
            }

            setupTruststore(configData);

            // Set fields so ensureAuthInfo/BaseConfigCredentialsCmd can use them
            if (server == null) {
                server = configData.getServerUrl();
            }
            if (realm == null) {
                realm = configData.getRealm();
            }

            configData = ensureAuthInfo(configData);
            configData = copyWithServerInfo(configData);

            String token = null;
            if (credentialsAvailable(configData)) {
                token = ensureToken(configData);
            }

            String requestRealm = getTargetRealm(configData);
            if (requestRealm == null) {
                requestRealm = DEFAULT_REALM;
            }
            configData.setRealm(requestRealm);

            String url = buildUrl(configData);
            String body = buildRequestBody();

            Headers headers = new Headers();
            if (token != null) {
                headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            headers.add(HttpHeaders.ACCEPT, APPLICATION_JSON);

            InputStream bodyStream = null;
            if (body != null) {
                String contentType = "PATCH".equals(descriptor.getHttpMethod())
                        ? MERGE_PATCH_JSON : APPLICATION_JSON;
                headers.add(HttpHeaders.CONTENT_TYPE, contentType);
                bodyStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            }

            HeadersBodyStatus response = HttpUtil.doRequest(
                    descriptor.getHttpMethod().toLowerCase(),
                    url,
                    new HeadersBody(headers, bodyStream));

            response.checkSuccess();
            String responseBody = response.getBody() != null ? readFully(response.getBody()) : "";

            if (!responseBody.isBlank()) {
                out.println(formatOutput(responseBody));
            } else if ("delete".equals(descriptor.getName()) && !descriptor.isHasResponseBody()) {
                out.println(capitalize(descriptor.getResourceName()) + " deleted");
            }
        } catch (CommandLine.ExecutionException e) {
            throw e;
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new CommandLine.ExecutionException(spec.commandLine(), message, e);
        }
    }

    private String buildUrl(ConfigData configData) {
        String path = descriptor.getPath()
                .replace("{realmName}", HttpUtil.urlencode(configData.getRealm()))
                .replace("{version}", API_VERSION);

        if (descriptor.isRequiresId()) {
            var positional = spec.commandLine().getParseResult().matchedPositional(0);
            if (positional == null) {
                throw new RuntimeException("Missing required ID argument");
            }
            path = path.replace(KcAdmV2DescriptorBuilder.ID_PATH_PARAM, HttpUtil.urlencode(positional.getValue()));
        }

        return configData.getServerUrl() + path;
    }

    private String buildRequestBody() throws IOException {
        String file = spec.commandLine().getParseResult()
                .matchedOptionValue(KcAdmV2CommandBuilder.OPT_FILE, null);

        List<OptionDescriptor> options = variant != null
                ? variant.getOptions() : descriptor.getOptions();

        if (file != null) {
            if (hasAnyFieldOptionSet(options)) {
                throw new RuntimeException(
                        "Options -f/--file and field options are mutually exclusive");
            }
            Path filePath = new File(file).toPath();
            if (!Files.isRegularFile(filePath)) {
                throw new RuntimeException("File not found: " + file);
            }
            return Files.readString(filePath);
        }
        if (options == null || options.isEmpty()) {
            return null;
        }

        boolean anyFieldSet = false;
        Map<String, Object> fields = new LinkedHashMap<>();

        if (variant != null) {
            fields.put(variant.getDiscriminatorField(), variant.getDiscriminatorValue());
            anyFieldSet = true;
        }

        for (OptionDescriptor opt : options) {
            Object value = spec.commandLine().getParseResult()
                    .matchedOptionValue("--" + opt.getName(), null);
            if (value != null) {
                anyFieldSet = true;
                Object converted;
                if (opt.isArray() && value instanceof String[]) {
                    converted = List.of((String[]) value);
                } else if (OptionDescriptor.TYPE_BOOLEAN.equals(opt.getType())) {
                    converted = Boolean.parseBoolean(value.toString());
                } else {
                    converted = value;
                }

                if (opt.getParentFieldName() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nested = (Map<String, Object>)
                            fields.computeIfAbsent(opt.getParentFieldName(), k -> new LinkedHashMap<>());
                    nested.put(opt.getFieldName(), converted);
                } else {
                    fields.put(opt.getFieldName(), converted);
                }
            }
        }

        if (!anyFieldSet) {
            return null;
        }

        return JsonSerialization.writeValueAsString(fields);
    }

    private boolean hasAnyFieldOptionSet(List<OptionDescriptor> options) {
        if (options == null) {
            return false;
        }
        for (OptionDescriptor opt : options) {
            if (spec.commandLine().getParseResult()
                    .matchedOptionValue("--" + opt.getName(), null) != null) {
                return true;
            }
        }
        return false;
    }

    private String formatOutput(String json) {
        try {
            var parseResult = spec.commandLine().getParseResult();
            boolean compressed = parseResult.matchedOptionValue(KcAdmV2CommandBuilder.OPT_COMPRESSED, false);

            JsonNode node = OutputUtil.MAPPER.readTree(json);

            String jsonOutput = compressed ? node.toString() : OutputUtil.MAPPER.writeValueAsString(node);

            if (CommandLine.Help.Ansi.AUTO.enabled()) {
                try {
                    return CliJsonOutputHighlighter.highlight(jsonOutput);
                } catch (IOException e) {
                    if (Globals.dumpTrace) {
                        System.err.println("Syntax highlighting failed, falling back to plain output: " + e.getMessage());
                    }
                }
            }

            return jsonOutput;
        } catch (Exception e) {
            throw new RuntimeException("Error processing results: " + e.getMessage(), e);
        }
    }
}
