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

import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfig;

import java.io.PrintWriter;
import java.nio.file.FileSystemException;
import java.util.Optional;
import org.jboss.logging.Logger;
import org.keycloak.platform.Platform;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.InitializationException;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.integration.QuarkusPlatform;

import io.smallrye.config.ConfigValue;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

public final class ExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private Logger logger;
    private boolean verbose;

    public ExecutionExceptionHandler() {}

    @Override
    public int handleExecutionException(Exception cause, CommandLine cmd, ParseResult parseResult) {
        error(cmd.getErr(), "Failed to run '" + parseResult.subcommands().stream()
                .map(ParseResult::commandSpec)
                .map(CommandLine.Model.CommandSpec::name)
                .findFirst()
                .orElse(Environment.getCommand()) + "' command.", cause);
        return cmd.getCommandSpec().exitCodeOnExecutionException();
    }

    public void error(PrintWriter errorWriter, String message, Throwable cause) {
        if (message != null) {
            logError(errorWriter, "ERROR: " + message);
        }

        if (cause != null) {
            if (cause instanceof InitializationException) {
                InitializationException initializationException = (InitializationException) cause;
                if (initializationException.getSuppressed() == null || initializationException.getSuppressed().length == 0) {
                    dumpException(errorWriter, initializationException);
                } else if (initializationException.getSuppressed().length == 1) {
                    dumpException(errorWriter, initializationException.getSuppressed()[0]);
                } else {
                    logError(errorWriter, "ERROR: Multiple configuration errors during startup");
                    int counter = 0;
                    for (Throwable inner : initializationException.getSuppressed()) {
                        counter++;
                        logError(errorWriter, "ERROR " + counter);
                        dumpException(errorWriter, inner);
                    }
                }
            } else {
                dumpException(errorWriter, cause);
            }

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
                printErrorHints(errorWriter, cause);
            } while ((cause = cause.getCause()) != null);
        }

        printErrorHints(errorWriter, cause);
    }

    private void printErrorHints(PrintWriter errorWriter, Throwable cause) {
        if (cause instanceof FileSystemException) {
            FileSystemException fse = (FileSystemException) cause;
            ConfigValue httpsCertFile = getConfig().getConfigValue("kc.https-certificate-file");

            if (fse.getFile().equals(Optional.ofNullable(httpsCertFile.getValue()).orElse(null))) {
                logError(errorWriter, Messages.httpsConfigurationNotSet().getMessage());
            }
        }
    }

    private void logError(PrintWriter errorWriter, String errorMessage) {
        logError(errorWriter, errorMessage, null);
    }

    // The "cause" can be null
    private void logError(PrintWriter errorWriter, String errorMessage, Throwable cause) {
        QuarkusPlatform platform = (QuarkusPlatform) Platform.getPlatform();
        if (platform.isStarted()) {
            // Can delegate to proper logger once the platform is started
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

    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(ExecutionExceptionHandler.class);
        }
        return logger;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
