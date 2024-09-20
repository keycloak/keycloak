/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.client.cli.common;

import java.io.PrintWriter;

import picocli.CommandLine;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

public class ShortErrorMessageHandler implements IParameterExceptionHandler {

    @Override
    public int handleParseException(ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        return shortErrorMessage(ex, cmd);
    }

    static int shortErrorMessage(Exception ex, CommandLine cmd) {
        PrintWriter writer = cmd.getErr();
        String errorMessage = ex.getMessage();

        writer.println(cmd.getColorScheme().errorText(errorMessage));
        if (ex instanceof ParameterException) {
            UnmatchedArgumentException.printSuggestions((ParameterException)ex, writer);
        }

        if (ex instanceof ParameterException || ex instanceof IllegalArgumentException) {
            CommandSpec spec = cmd.getCommandSpec();
            writer.printf("Try '%s%s' for more information on the available options.%n", spec.qualifiedName(), "help".equals(spec.name())?"":" --help");
            return cmd.getCommandSpec().exitCodeOnInvalidInput();
        }
        return cmd.getCommandSpec().exitCodeOnExecutionException();
    }

}
