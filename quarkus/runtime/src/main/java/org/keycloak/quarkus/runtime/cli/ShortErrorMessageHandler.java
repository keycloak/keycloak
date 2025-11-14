package org.keycloak.quarkus.runtime.cli;

import java.io.PrintWriter;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.configuration.KcUnmatchedArgumentException;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import picocli.CommandLine;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

import static java.lang.String.format;

import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;

public class ShortErrorMessageHandler implements IParameterExceptionHandler {

    @Override
    public int handleParseException(ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        PrintWriter writer = cmd.getErr();
        String errorMessage = ex.getMessage();
        String additionalSuggestion = null;

        if (ex instanceof UnmatchedArgumentException uae) {
            String[] unmatched = getUnmatchedPartsByOptionSeparator(uae, "=");

            String cliKey = unmatched[0];

            PropertyMapper<?> mapper = PropertyMappers.getMapperByCliKey(cliKey);

            Optional<PropertyMapper<?>> disabled = PropertyMappers.getKcKeyFromCliKey(cliKey).flatMap(PropertyMappers::getDisabledMapper);

            final BooleanSupplier isUnknownOption = () -> mapper == null || !(cmd.getCommand() instanceof AbstractCommand);

            if (mapper == null && disabled.isPresent()) {
                var enabledWhen = disabled
                        .flatMap(PropertyMapper::getEnabledWhen)
                        .map(desc -> format(". %s", desc))
                        .orElse("");

                errorMessage = format("Disabled option: '%s'%s", cliKey, enabledWhen);
                additionalSuggestion = "Specify '--help-all' to obtain information on all options and their availability.";
            } else if (isUnknownOption.getAsBoolean()) {
                if (cliKey.split("\\s").length > 1) {
                    errorMessage = "Option: '" + cliKey + "' is not expected to contain whitespace, please remove any unnecessary quoting/escaping";
                } else {
                    errorMessage = "Unknown option: '" + cliKey + "'";
                }
            } else {
                AbstractCommand command = cmd.getCommand();
                if (!command.getOptionCategories().contains(mapper.getCategory())) {
                    errorMessage = format("Option: '%s' not valid for command %s", cliKey, cmd.getCommandName());
                } else {
                    if (Stream.of(args).anyMatch(OPTIMIZED_BUILD_OPTION_LONG::equals) && mapper.isBuildTime() && Start.NAME.equals(cmd.getCommandName())) {
                        errorMessage = format("Build time option: '%s' not usable with pre-built image and --optimized", cliKey);
                    } else {
                        final var optionType = mapper.isRunTime() ? "Run time" : "Build time";
                        errorMessage = format("%s option: '%s' not usable with %s", optionType, cliKey, cmd.getCommandName());
                    }
                }
            }
        } else if (ex instanceof MissingParameterException mpe) {
            if (mpe.getMissing().size() == 1) {
                ArgSpec spec = mpe.getMissing().get(0);
                if (spec instanceof OptionSpec option) {
                    errorMessage = getExpectedMessage(option);
                }
            }
        }

        writer.println(cmd.getColorScheme().errorText(errorMessage));
        if (!(ex instanceof KcUnmatchedArgumentException) && ex instanceof UnmatchedArgumentException) {
            ex = new KcUnmatchedArgumentException((UnmatchedArgumentException) ex);
        }
        UnmatchedArgumentException.printSuggestions(ex, writer);

        CommandSpec spec = cmd.getCommandSpec();
        writer.printf("Try '%s --help' for more information on the available options.%n", spec.qualifiedName());

        if (additionalSuggestion != null) {
            writer.println(additionalSuggestion);
        }

        return getInvalidInputExitCode(ex, cmd);
    }

    static int getInvalidInputExitCode(Throwable ex, CommandLine cmd) {
        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : cmd.getCommandSpec().exitCodeOnInvalidInput();
    }

    private String[] getUnmatchedPartsByOptionSeparator(UnmatchedArgumentException uae, String separator) {
        return uae.getUnmatched().get(0).split(separator);
    }

    private String getExpectedMessage(OptionSpec option) {
        return String.format("Option '%s' (%s) expects %s.%s", String.join(", ", option.names()), option.paramLabel(),
                option.typeInfo().isMultiValue() ? "one or more comma separated values without whitespace": "a single value",
                getExpectedValuesMessage(option.completionCandidates(), isCaseInsensitive(option)));
    }

    private boolean isCaseInsensitive(OptionSpec option) {
        if (option.longestName().startsWith("--")) {
            var mapper = PropertyMappers.getMapper(option.longestName().substring(2));
            if (mapper != null) {
                return mapper.getOption().isCaseInsensitiveExpectedValues();
            }
        }
        return false;
    }

    public static String getExpectedValuesMessage(Iterable<String> specCandidates, boolean caseInsensitive) {
        if (specCandidates == null || !specCandidates.iterator().hasNext()) {
            return "";
        }
        return String.format(" Expected values are%s: %s", caseInsensitive ? " (case insensitive)" : "",
                String.join(", ", specCandidates));
    }

}
