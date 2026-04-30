package org.keycloak.client.admin.cli.v2;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.CommandDescriptor;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.util.OutputUtil;

import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import static org.keycloak.client.admin.cli.KcAdmMain.CMD;
import static org.keycloak.client.admin.cli.KcAdmMain.V2_FLAG;
import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.HttpUtil.APPLICATION_JSON;
import static org.keycloak.client.cli.util.IoUtil.readFully;
import static org.keycloak.client.cli.util.OsUtil.OS_ARCH;

@Command
final class KcAdmV2EditCmd extends KcAdmV2RequestExecutor {

    private static final String ENV_KC_CLI_EDITOR = "KC_CLI_EDITOR";
    private static final String ENV_VISUAL = "VISUAL";
    private static final String ENV_EDITOR = "EDITOR";
    private static final String DEFAULT_EDITOR_UNIX = "vi";
    private static final String DEFAULT_EDITOR_WINDOWS = "notepad";
    private static final int EXIT_CODE_COMMAND_NOT_FOUND = 127;

    private final String resourceName;
    private final String getMethod;
    private final String putMethod;

    KcAdmV2EditCmd(CommandDescriptor getDescriptor, CommandDescriptor putDescriptor) {
        super(getDescriptor, null);
        this.resourceName = getDescriptor.getResourceName();
        this.getMethod = getDescriptor.getHttpMethod().toLowerCase();
        this.putMethod = putDescriptor.getHttpMethod().toLowerCase();
    }

    @Override
    public void run() {
        if (Globals.help) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return;
        }

        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        try {
            RequestContext ctx = prepareRequest();
            String url = buildUrl(ctx.configData(), null);

            String originalJson = readFully(executeRequest(getMethod, url, ctx.token(), null, null).getBody());
            JsonNode originalNode = OutputUtil.MAPPER.readTree(originalJson);

            String editor = resolveEditor(ctx.configData());
            String prettyOriginal = OutputUtil.MAPPER.writeValueAsString(originalNode);
            String modifiedContent = openInEditor(editor, prettyOriginal);

            final JsonNode modifiedNode;
            try {
                modifiedNode = OutputUtil.MAPPER.readTree(modifiedContent);
            } catch (Exception e) {
                throw new RuntimeException("Modified content is not valid JSON: " + e.getMessage());
            }

            if (originalNode.equals(modifiedNode)) {
                err.println("Edit cancelled, no changes made.");
                return;
            }

            String id = spec.commandLine().getParseResult().matchedPositional(0).getValue();
            err.println("Updating " + resourceName + " '" + id + "' ...");
            err.println();

            String freshToken = credentialsAvailable(ctx.configData()) ? ensureToken(ctx.configData()) : ctx.token();
            String responseBody = readFully(
                    executeRequest(putMethod, url, freshToken, modifiedContent, APPLICATION_JSON).getBody());
            out.println(formatOutput(responseBody));
        } catch (CommandLine.ExecutionException e) {
            throw e;
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new CommandLine.ExecutionException(spec.commandLine(), message, e);
        }
    }

    private String resolveEditor(ConfigData configData) {
        String editor = configData.getEditor();
        if (editor != null && !editor.isBlank()) {
            return editor;
        }

        editor = System.getenv(ENV_KC_CLI_EDITOR);
        if (editor != null && !editor.isBlank()) {
            return editor;
        }

        editor = System.getenv(ENV_VISUAL);
        if (editor != null && !editor.isBlank()) {
            return editor;
        }

        editor = System.getenv(ENV_EDITOR);
        if (editor != null && !editor.isBlank()) {
            return editor;
        }

        return OS_ARCH.isWindows() ? DEFAULT_EDITOR_WINDOWS : DEFAULT_EDITOR_UNIX;
    }

    private String openInEditor(String editor, String content) throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile("kcadm-edit-", ".json");
        try {
            Files.writeString(tempFile, content);

            ProcessBuilder pb;
            if (OS_ARCH.isWindows()) {
                pb = new ProcessBuilder("cmd", "/c", editor + " \"" + tempFile + "\"");
            } else {
                pb = new ProcessBuilder("/bin/sh", "-c", editor + " \"" + tempFile + "\"");
            }
            pb.inheritIO();

            int exitCode = pb.start().waitFor();
            if (exitCode == EXIT_CODE_COMMAND_NOT_FOUND) {
                throw new RuntimeException("Editor '" + editor + "' not found. "
                        + "Configure your editor with: " + KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG
                        + " config editor <editor> or set the " + ENV_KC_CLI_EDITOR + " environment variable.");
            }
            if (exitCode != 0) {
                throw new RuntimeException("Editor exited with error (exit code " + exitCode + "). "
                        + "Verify your editor works correctly or reconfigure with: "
                        + KcAdmMain.CMD + " " + KcAdmMain.V2_FLAG + " config editor <editor>");
            }

            return Files.readString(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    static String[] createDescription(String resourceName) {
        return new String[] {
                "Edit a " + resourceName + " by opening it in a text editor and applying the modifications.",
                "Editor is resolved from (in order): '" + CMD + " " + V2_FLAG + " config editor' setting,",
                ENV_KC_CLI_EDITOR + ", " + ENV_VISUAL + ", " + ENV_EDITOR + " environment variables, or "
                        + DEFAULT_EDITOR_UNIX + " (" + DEFAULT_EDITOR_WINDOWS + " on Windows)."
        };
    }
}
