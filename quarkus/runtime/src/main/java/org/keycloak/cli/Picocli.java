/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cli;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.configuration.PropertyMapper;
import org.keycloak.configuration.PropertyMappers;
import org.keycloak.platform.Platform;
import org.keycloak.provider.quarkus.InitializationException;
import org.keycloak.provider.quarkus.QuarkusPlatform;
import org.keycloak.util.Environment;
import picocli.CommandLine;

final class Picocli {

    private static final Logger logger = Logger.getLogger(Picocli.class);

    static CommandLine createCommandLine() {
        CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.forAnnotatedObject(new MainCommand())
                .name(Environment.getCommand());

        addOption(spec, "start", PropertyMappers.getRuntimeMappers());
        addOption(spec, "start-dev", PropertyMappers.getRuntimeMappers());
        addOption(spec, "config", PropertyMappers.getRuntimeMappers());
        addOption(spec, "config", PropertyMappers.getBuiltTimeMappers());
        addOption(spec.subcommands().get("config").getCommandSpec(), "--features", "Enables a group of features. Possible values are: "
                + String.join(",", Arrays.asList(Profile.Type.values()).stream().map(
                type -> type.name().toLowerCase()).toArray((IntFunction<CharSequence[]>) String[]::new)));

        for (Profile.Feature feature : Profile.Feature.values()) {
            addOption(spec.subcommands().get("config").getCommandSpec(), "--features-" + feature.name().toLowerCase(),
                    "Enables the " + feature.name() + " feature. Set enabled to enable the feature or disabled otherwise.");
        }
        
        CommandLine cmd = new CommandLine(spec);

        cmd.setExecutionExceptionHandler(new CommandLine.IExecutionExceptionHandler() {
            @Override
            public int handleExecutionException(Exception ex, CommandLine commandLine,
                    CommandLine.ParseResult parseResult) {
                commandLine.getErr().println(ex.getMessage());
                commandLine.usage(commandLine.getErr());
                return commandLine.getCommandSpec().exitCodeOnExecutionException();
            }
        });
        
        return cmd;
    }

    static String parseConfigArgs(List<String> argsList) {
        StringBuilder options = new StringBuilder();
        Iterator<String> iterator = argsList.iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();

            // TODO: ignore properties for providers for now, need to fetch them from the providers, otherwise CLI will complain about invalid options
            if (key.startsWith("--spi")) {
                iterator.remove();
            }

            if (key.startsWith("--")) {
                if (options.length() > 0) {
                    options.append(",");
                }
                options.append(key);
            }
        }

        return options.toString();
    }

    private static void addOption(CommandLine.Model.CommandSpec spec, String command, List<PropertyMapper> mappers) {
        CommandLine.Model.CommandSpec commandSpec = spec.subcommands().get(command).getCommandSpec();

        for (PropertyMapper mapper : mappers) {
            String name = "--" + PropertyMappers.toCLIFormat(mapper.getFrom()).substring(3);
            String description = mapper.getDescription();

            if (description == null || commandSpec.optionsMap().containsKey(name)) {
                continue;
            }

            addOption(commandSpec, name, description);
        }
    }

    private static void addOption(CommandLine.Model.CommandSpec commandSpec, String name, String description) {
        commandSpec.addOption(CommandLine.Model.OptionSpec.builder(name)
                .description(description)
                .paramLabel("<value>")
                .type(String.class).build());
    }

    static List<String> getCliArgs(CommandLine cmd) {
        CommandLine.ParseResult parseResult = cmd.getParseResult();

        if (parseResult == null) {
            return Collections.emptyList();
        }

        return parseResult.expandedArgs();
    }

    static void error(List<String> cliArgs, PrintWriter errorWriter, String message, Throwable throwable) {
        logError(errorWriter, "ERROR: " + message);

        if (throwable != null) {
            boolean verbose = cliArgs.stream().anyMatch((arg) -> "--verbose".equals(arg));

            if (throwable instanceof InitializationException) {
                InitializationException initializationException = (InitializationException) throwable;
                if (initializationException.getSuppressed() == null || initializationException.getSuppressed().length == 0) {
                    dumpException(errorWriter, initializationException, verbose);
                } else if (initializationException.getSuppressed().length == 1) {
                    dumpException(errorWriter, initializationException.getSuppressed()[0], verbose);
                } else {
                    logError(errorWriter, "ERROR: Multiple configuration errors during startup");
                    int counter = 0;
                    for (Throwable inner : initializationException.getSuppressed()) {
                        counter++;
                        logError(errorWriter, "ERROR " + counter);
                        dumpException(errorWriter, inner, verbose);
                    }
                }
            } else {
                dumpException(errorWriter, throwable, verbose);
            }

            if (!verbose) {
                logError(errorWriter, "For more details run the same command passing the '--verbose' option. Also you can use '--help' to see the details about the usage of the particular command.");
            }
        }

        System.exit(1);
    }

    static void error(CommandLine cmd, String message, Throwable throwable) {
        error(getCliArgs(cmd), cmd.getErr(), message, throwable);
    }

    static void error(CommandLine cmd, String message) {
        error(getCliArgs(cmd), cmd.getErr(), message, null);
    }

    static void println(CommandLine cmd, String message) {
        cmd.getOut().println(message);
    }

    private static void dumpException(PrintWriter errorWriter, Throwable cause, boolean verbose) {
        if (verbose) {
            logError(errorWriter, "ERROR: Details:", cause);
        } else {
            do {
                if (cause.getMessage() != null) {
                    logError(errorWriter, String.format("ERROR: %s", cause.getMessage()));
                }
            } while ((cause = cause.getCause())!= null);
        }
    }

    private static void logError(PrintWriter errorWriter, String errorMessage) {
        logError(errorWriter, errorMessage, null);
    }

    // The "cause" can be null
    private static void logError(PrintWriter errorWriter, String errorMessage, Throwable cause) {
        QuarkusPlatform platform = (QuarkusPlatform) Platform.getPlatform();
        if (platform.isStarted()) {
            // Can delegate to proper logger once the platform is started
            if (cause == null) {
                logger.error(errorMessage);
            } else {
                logger.error(errorMessage, cause);
            }
        } else {
            if (cause == null) {
                errorWriter.println(errorMessage);
            } else {
                errorWriter.println(errorMessage);
                cause.printStackTrace();
            }
        }
    }
}
