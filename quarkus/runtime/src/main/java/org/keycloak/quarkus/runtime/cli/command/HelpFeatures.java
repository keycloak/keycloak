package org.keycloak.quarkus.runtime.cli.command;

import io.quarkus.dev.console.QuarkusConsole;
import org.keycloak.common.Profile;
import org.keycloak.quarkus.runtime.QuarkusProfileConfigResolver;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = HelpFeatures.NAME, header = HelpFeatures.HEADER, description = "%n" + HelpFeatures.HEADER)
public class HelpFeatures extends AbstractCommand {
    public static final String NAME = "features";
    public static final String HEADER = "Information about features";
    private static final List<Profile.Feature.Type> IGNORED_TYPES_ENABLED = List.of(Profile.Feature.Type.DEFAULT);
    private static final List<Profile.Feature.Type> IGNORED_TYPES_DISABLED = List.of(Profile.Feature.Type.DISABLED_BY_DEFAULT);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void runCommand() {
        Profile.configure(new QuarkusProfileConfigResolver());
        var out = spec.commandLine().getOut();
        var ansi = QuarkusConsole.hasColorSupport() ? CommandLine.Help.Ansi.ON : CommandLine.Help.Ansi.OFF;
        var enabled = new ArrayList<Profile.Feature>();
        var disabled = new ArrayList<Profile.Feature>();

        for (var f : Profile.Feature.values()) {
            if (Profile.isFeatureEnabled(f)) {
                enabled.add(f);
            } else {
                disabled.add(f);
            }
        }

        printSection(out, ansi, "Enabled features:", enabled, true);
        out.println();
        printSection(out, ansi, "Disabled features:", disabled, false);

        System.exit(0);
    }

    private void printSection(PrintWriter out, CommandLine.Help.Ansi ansi, String title,
                              List<Profile.Feature> features, boolean toEnable) {
        out.println(ansi.string("@|bold " + title + "|@"));
        features.forEach(f -> {
            String typeDisplay = "";
            boolean showType = toEnable ? !IGNORED_TYPES_ENABLED.contains(f.getType()) : !IGNORED_TYPES_DISABLED.contains(f.getType());
            if (showType) {
                typeDisplay = " (" + colorizeType(f.getType(), ansi) + ")";
            }

            String key = toEnable ? f.getVersionedKey() : f.getUnversionedKey();
            String formattedKey = ansi.string("@|bold " + key + "|@");
            out.printf("    %s%s%n", formattedKey, typeDisplay);
            out.printf("      %s%n", f.getLabel());
        });
    }

    private String colorizeType(Profile.Feature.Type type, CommandLine.Help.Ansi ansi) {
        String label = type.getLabel();
        String color = switch (type) {
            case PREVIEW, PREVIEW_DISABLED_BY_DEFAULT -> "yellow";
            case EXPERIMENTAL -> "magenta";
            case DEPRECATED, DISABLED_BY_DEFAULT -> "red";
            case DEFAULT -> "green";
        };
        return ansi.string("@|" + color + " " + label + "|@");
    }
}
