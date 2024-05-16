package org.keycloak.quarkus.runtime.cli;

import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.configuration.KcUnmatchedArgumentException;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import java.io.PrintWriter;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import static java.lang.String.format;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG;

public class ShortErrorMessageHandler implements IParameterExceptionHandler {

    @Override
    public int handleParseException(ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        PrintWriter writer = cmd.getErr();
        String errorMessage = ex.getMessage();
        String additionalSuggestion = null;

        if (ex instanceof UnmatchedArgumentException) {
            UnmatchedArgumentException uae = (UnmatchedArgumentException) ex;

            String[] unmatched = getUnmatchedPartsByOptionSeparator(uae, "=");

            String cliKey = unmatched[0];

            PropertyMapper<?> mapper = PropertyMappers.getMapper(cliKey);

            final boolean isDisabledOption = mapper == null && PropertyMappers.isDisabledMapper(cliKey);
            final BooleanSupplier isUnknownOption = () -> mapper == null || !(cmd.getCommand() instanceof AbstractCommand);

            if (isDisabledOption) {
                var enabledWhen = PropertyMappers.getDisabledMapper(cliKey)
                        .map(PropertyMapper::getEnabledWhen)
                        .filter(Optional::isPresent)
                        .map(desc -> format(". %s", desc.get()))
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

    static int getInvalidInputExitCode(Exception ex, CommandLine cmd) {
        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : cmd.getCommandSpec().exitCodeOnInvalidInput();
    }

    private String[] getUnmatchedPartsByOptionSeparator(UnmatchedArgumentException uae, String separator) {
        return uae.getUnmatched().get(0).split(separator);
    }
}
