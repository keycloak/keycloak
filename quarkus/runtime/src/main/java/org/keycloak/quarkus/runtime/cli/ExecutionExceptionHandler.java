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
import java.nio.file.FileSystemException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.keycloak.platform.Platform;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.integration.QuarkusPlatform;

import io.smallrye.config.ConfigValue;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfig;

public final class ExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private static Logger logger;
    private boolean verbose;
    private static Map<String, Function<Throwable, Throwable>> exceptionTransformers = new HashMap<>();

    public ExecutionExceptionHandler() {}

    @Override
    public int handleExecutionException(Exception cause, CommandLine cmd, ParseResult parseResult) {
        var exception = handleExceptionTransformers(cause);
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
        var exception = handleExceptionTransformers(cause);
        if (message != null) {
            logError(errorWriter, "ERROR: " + message);
        }

        if (exception != null) {
            dumpException(errorWriter, exception);

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
                logError(errorWriter, Messages.httpsConfigurationNotSet());
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

    private static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(ExecutionExceptionHandler.class);
        }
        return logger;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public static void addExceptionTransformer(Class<?> fromClass, Function<Throwable, Throwable> transformer) {
        if (exceptionTransformers.get(fromClass.getName()) != null) {
            getLogger().warnf("Transformer for the '%s' class is overridden", fromClass.getName());
        }
        exceptionTransformers.put(fromClass.getName(), transformer);
    }

    public static void resetExceptionTransformers() {
        exceptionTransformers = new HashMap<>();
    }

    private static Throwable handleExceptionTransformers(Throwable exception) {
        if (exception == null) {
            return null;
        }

        if (exceptionTransformers.isEmpty()) {
            return exception;
        }

        var stackTrace = exception.getStackTrace();
        for (var trace : stackTrace) {
            var transformer = exceptionTransformers.get(trace.getClassName());
            if (transformer != null) {
                return transformer.apply(exception);
            }
        }
        return exception;
    }
}
