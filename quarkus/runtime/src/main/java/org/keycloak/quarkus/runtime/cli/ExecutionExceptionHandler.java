/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.cli;

import java.io.PrintWriter;

import org.keycloak.quarkus.runtime.Environment;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

public final class ExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private static Logger logger;
    private boolean verbose;

    public ExecutionExceptionHandler() {}

    @Override
    public int handleExecutionException(Exception cause, CommandLine cmd, ParseResult parseResult) {
        var exception = cause;
        if (exception instanceof PropertyException) {
            PrintWriter writer = cmd.getErr();
            writer.println(cmd.getColorScheme().errorText(exception.getMessage()));
            if (verbose && exception.getCause() != null) {
                dumpException(writer, exception.getCause());
            }
            return ShortErrorMessageHandler.getInvalidInputExitCode(exception, cmd);
        }
        error(cmd.getErr(), "Failed to run '" + parseResult.subcommands().stream()
                .map(ParseResult::commandSpec)
                .map(CommandLine.Model.CommandSpec::name)
                .findFirst()
                .orElse(Environment.getCommand()) + "' command.", exception);
        return cmd.getCommandSpec().exitCodeOnExecutionException();
    }

    public void error(PrintWriter errorWriter, String message, Throwable cause) {
        if (message != null) {
            logError(errorWriter, "ERROR: " + message);
        }

        if (cause != null) {
            dumpException(errorWriter, cause);

            if (!verbose) {
                logError(errorWriter, "For more details run the same command passing the '--verbose' option. Also you can use '--help' to see the details about the usage of the particular command.");
            }
        }
    }

    private void dumpException(PrintWriter errorWriter, Throwable cause) {
        if (verbose) {
            logError(errorWriter, cause == null ? "Unknown error." : "Error details:", cause);
        } else {
            do {
                if (cause.getMessage() != null) {
                    logError(errorWriter, String.format("ERROR: %s", cause.getMessage()));
                }
            } while ((cause = cause.getCause()) != null);
        }
    }

    private void logError(PrintWriter errorWriter, String errorMessage) {
        logError(errorWriter, errorMessage, null);
    }

    // The "cause" can be null
    private void logError(PrintWriter errorWriter, String errorMessage, Throwable cause) {
        if (InitialConfigurator.DELAYED_HANDLER.isActivated()) {
            // Can delegate to proper logger once delayed handler is activated
            if (cause == null) {
                getLogger().error(errorMessage);
            } else {
                getLogger().error(errorMessage, cause);
            }
        } else {
            if (cause == null) {
                errorWriter.println(errorMessage);
            } else {
                errorWriter.println(errorMessage);
                cause.printStackTrace(errorWriter);
            }
        }
    }

    private static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(ExecutionExceptionHandler.class);
        }
        return logger;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

}
