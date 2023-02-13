package org.keycloak.quarkus.runtime.cli;

import picocli.CommandLine;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;

public class ShortErrorMessageHandler implements IParameterExceptionHandler {

    public int handleParseException(ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        PrintWriter writer = cmd.getErr();
        String errorMessage = ex.getMessage();

        if (ex instanceof UnmatchedArgumentException) {
            UnmatchedArgumentException uae = (UnmatchedArgumentException) ex;

            String[] unmatched = getUnmatchedPartsByOptionSeparator(uae,"=");
            String original = uae.getUnmatched().get(0);

            if (unmatched[0].equals(original)) {
                unmatched = getUnmatchedPartsByOptionSeparator(uae," ");
            }

            errorMessage = "Unknown option: '" + unmatched[0] + "'";
        }

        writer.println(cmd.getColorScheme().errorText(errorMessage));
        UnmatchedArgumentException.printSuggestions(ex, writer);

        CommandSpec spec = cmd.getCommandSpec();
        writer.printf("Try '%s --help' for more information on the available options.%n", spec.qualifiedName());

        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : spec.exitCodeOnInvalidInput();
    }

    private String[] getUnmatchedPartsByOptionSeparator(UnmatchedArgumentException uae, String separator) {
        return uae.getUnmatched().get(0).split(separator);
    }
}
