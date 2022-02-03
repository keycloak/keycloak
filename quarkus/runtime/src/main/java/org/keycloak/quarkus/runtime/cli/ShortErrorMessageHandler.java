package org.keycloak.quarkus.runtime.cli;

import picocli.CommandLine;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;
import picocli.CommandLine.Help;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;

public class ShortErrorMessageHandler implements IParameterExceptionHandler {

    public int handleParseException(ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        PrintWriter writer = cmd.getErr();

        writer.println(cmd.getColorScheme().errorText(ex.getMessage()));
        UnmatchedArgumentException.printSuggestions(ex, writer);

        CommandSpec spec = cmd.getCommandSpec();
        writer.printf("Try '%s --help' for more information on the available options.%n", spec.qualifiedName());

        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : spec.exitCodeOnInvalidInput();
    }
}
