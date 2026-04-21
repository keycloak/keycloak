package org.keycloak.client.admin.cli.v2;

import java.util.List;

import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.OptionDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.ResourceDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.VariantDescriptor;

import picocli.CommandLine;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

import static org.keycloak.client.admin.cli.KcAdmMain.CMD;
import static org.keycloak.client.admin.cli.KcAdmMain.V2_FLAG;
import static org.keycloak.common.util.ObjectUtil.capitalize;

class KcAdmV2CommandBuilder {

    private static final String OPT_HELP = "--help";
    static final String OPT_FILE = "-f";
    static final String OPT_COMPRESSED = "--compressed";


    static void addCommands(CommandLine cli, KcAdmV2CommandDescriptor descriptor) {
        for (ResourceDescriptor resource : descriptor.getResources()) {
            GroupCommand groupCommand = new GroupCommand(resource.getName());
            CommandSpec groupSpec = CommandSpec.wrapWithoutInspection(groupCommand);
            groupSpec.name(resource.getName());
            groupSpec.usageMessage().header(capitalize(resource.getName()) + " operations");
            addHelpOption(groupSpec);

            CommandLine groupCli = new CommandLine(groupSpec);
            groupCommand.setSpec(groupSpec);

            for (CommandDescriptor cmd : resource.getCommands()) {
                groupCli.addSubcommand(cmd.getName(), buildSubcommand(cmd));
            }

            cli.addSubcommand(resource.getName(), groupCli);
        }
    }

    private static CommandLine buildSubcommand(CommandDescriptor cmd) {
        if (cmd.hasVariants()) {
            return buildVariantParentCommand(cmd);
        }

        return buildLeafCommand(cmd, cmd.getOptions(), null);
    }

    private static CommandLine buildVariantParentCommand(CommandDescriptor cmd) {
        CommandLine parentCli = buildLeafCommand(cmd, null, null);

        for (VariantDescriptor variant : cmd.getVariants()) {
            parentCli.addSubcommand(variant.getName(),
                    buildLeafCommand(cmd, variant.getOptions(), variant));
        }

        return parentCli;
    }

    private static CommandLine buildLeafCommand(CommandDescriptor cmd,
            List<OptionDescriptor> options, VariantDescriptor variant) {
        boolean isVariantParent = variant == null && cmd.hasVariants();

        KcAdmV2RequestExecutor executor = new KcAdmV2RequestExecutor(cmd, variant);
        CommandSpec spec = CommandSpec.forAnnotatedObject(executor);
        spec.name(variant != null ? variant.getName() : cmd.getName());
        spec.usageMessage().description(cmd.getDescription());
        spec.usageMessage().optionListHeading("%nConnection options:%n");

        // Replace inherited --help with usageHelp=true so PicoCLI skips
        // required parameter validation when --help is present
        spec.remove(spec.findOption(OPT_HELP));
        addHelpOption(spec);

        if (cmd.isHasResponseBody()) {
            addOutputGroup(spec);
        }

        if (!isVariantParent && cmd.isRequiresId()) {
            spec.addPositional(PositionalParamSpec.builder()
                    .index("0")
                    .paramLabel("<id>")
                    .description("Resource identifier")
                    .required(true)
                    .type(String.class)
                    .build());
        }

        boolean hasFieldOptions = options != null && !options.isEmpty();
        if (hasFieldOptions || isVariantParent) {
            ArgGroupSpec.Builder fieldGroup = ArgGroupSpec.builder()
                    .heading("%nOptions:%n")
                    .exclusive(false)
                    .validate(false)
                    .order(1);

            fieldGroup.addArg(buildFileOption(hasFieldOptions));

            if (hasFieldOptions) {
                for (OptionDescriptor opt : options) {
                    fieldGroup.addArg(buildOption(opt));
                }
            }

            spec.addArgGroup(fieldGroup.build());
        }

        return new CommandLine(spec);
    }

    private static OptionSpec buildOption(OptionDescriptor opt) {
        String description = opt.getDescription() != null ? opt.getDescription() : "";
        List<String> enumValues = opt.getEnumValues();
        if (enumValues != null && !enumValues.isEmpty()) {
            description += (description.isEmpty() ? "" : " ") + "Valid values: " + String.join(", ", enumValues);
        }

        OptionSpec.Builder builder = OptionSpec.builder("--" + opt.getName())
                .type(opt.isArray() ? String[].class : String.class)
                .paramLabel("<value>")
                .description(description);

        if (opt.isArray()) {
            builder.splitRegex(",");
        }

        if (enumValues != null && !enumValues.isEmpty()) {
            builder.completionCandidates(enumValues);
        }

        return builder.build();
    }

    private static OptionSpec buildFileOption(boolean hasFieldOptions) {
        String description = hasFieldOptions
                ? "JSON file with request body (mutually exclusive with field options)"
                : "JSON file with request body";
        return OptionSpec.builder(OPT_FILE, "--file")
                .type(String.class)
                .paramLabel("<file>")
                .description(description)
                .build();
    }

    private static void addOutputGroup(CommandSpec spec) {
        spec.addArgGroup(ArgGroupSpec.builder()
                .heading("%nOutput options:%n")
                .exclusive(false)
                .validate(false)
                .order(10)
                .addArg(OptionSpec.builder("-c", OPT_COMPRESSED)
                        .type(boolean.class)
                        .description("Don't pretty print the output")
                        .build())
                .build());
    }

    private static void addHelpOption(CommandSpec spec) {
        spec.addOption(OptionSpec.builder("-h", OPT_HELP)
                .usageHelp(true)
                .hidden(true)
                .build());
    }

    static class GroupCommand implements Runnable {
        private final String name;
        private CommandSpec spec;

        GroupCommand(String name) {
            this.name = name;
        }

        void setSpec(CommandSpec spec) {
            this.spec = spec;
        }

        @Override
        public void run() {
            spec.commandLine().getErr().println(
                    "Use '" + CMD + " " + V2_FLAG + " " + name + " --help' for available commands.");
        }
    }
}
