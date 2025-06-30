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

package org.keycloak.quarkus.runtime;

import static org.keycloak.quarkus.runtime.Environment.getKeycloakModeFromProfile;
import static org.keycloak.quarkus.runtime.Environment.isDevProfile;
import static org.keycloak.quarkus.runtime.Environment.getProfileOrDefault;
import static org.keycloak.quarkus.runtime.Environment.isImportExportMode;
import static org.keycloak.quarkus.runtime.Environment.isTestLaunchMode;
import static org.keycloak.quarkus.runtime.cli.Picocli.parseAndRun;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG;
import static org.keycloak.quarkus.runtime.cli.command.Start.isDevProfileNotAllowed;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import jakarta.enterprise.context.ApplicationScoped;
import org.keycloak.common.profile.ProfileException;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import picocli.CommandLine.ExitCode;

import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.Quarkus;

import org.jboss.logging.Logger;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.common.Version;
import org.keycloak.quarkus.runtime.cli.command.Start;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * <p>The main entry point, responsible for initialize and run the CLI as well as start the server.
 */
@QuarkusMain(name = "keycloak")
@ApplicationScoped
public class KeycloakMain implements QuarkusApplication {

    public static void main(String[] args) {
        ensureForkJoinPoolThreadFactoryHasBeenSetToQuarkus();

        System.setProperty("kc.version", Version.VERSION);
        List<String> cliArgs = null;
        try {
            cliArgs = Picocli.parseArgs(args);
        } catch (PropertyException e) {
            handleUsageError(e.getMessage());
            return;
        }

        if (cliArgs.isEmpty()) {
            cliArgs = new ArrayList<>(cliArgs);
            // default to show help message
            cliArgs.add("-h");
        } else if (isFastStart(cliArgs)) { // fast path for starting the server without bootstrapping CLI

            if (isDevProfileNotAllowed()) {
                handleUsageError(Messages.devProfileNotAllowedError(Start.NAME));
                return;
            }

            Environment.setParsedCommand(new Start());

            try {
                PropertyMappers.sanitizeDisabledMappers();
                Picocli.validateConfig(cliArgs, new Start());
            } catch (PropertyException | ProfileException e) {
                handleUsageError(e.getMessage(), e.getCause());
                return;
            }

            ExecutionExceptionHandler errorHandler = new ExecutionExceptionHandler();
            PrintWriter errStream = new PrintWriter(System.err, true);

            start(errorHandler, errStream, args);

            return;
        }

        // parse arguments and execute any of the configured commands
        parseAndRun(cliArgs);
    }

    /**
     * Verify that the property for the ForkJoinPool factory set by Quarkus matches the actual factory.
     * If this is not the case, the classloader for those threads is not set correctly, and for example loading configurations
     * via SmallRye is unreliable. This can happen if a Java Agent or JMX initializes the ForkJoinPool before Java's main method is run.
     */
    private static void ensureForkJoinPoolThreadFactoryHasBeenSetToQuarkus() {
        // At this point, the settings from the CLI are no longer visible as they have been overwritten in the QuarkusEntryPoint.
        // Therefore, the only thing we can do is to check if the thread pool class name is the same as in the configuration.
        final String FORK_JOIN_POOL_COMMON_THREAD_FACTORY = "java.util.concurrent.ForkJoinPool.common.threadFactory";
        String sf = System.getProperty(FORK_JOIN_POOL_COMMON_THREAD_FACTORY);
        //noinspection resource
        if (!ForkJoinPool.commonPool().getFactory().getClass().getName().equals(sf)) {
            Logger.getLogger(KeycloakMain.class).errorf("The ForkJoinPool has been initialized with the wrong thread factory. The property '%s' should be set on the Java CLI to ensure Java's ForkJoinPool will always be initialized with '%s' even if there are Java agents which might initialize logging or other capabilities earlier than the main method.",
                    FORK_JOIN_POOL_COMMON_THREAD_FACTORY,
                    sf);
            throw new RuntimeException("The ForkJoinPool has been initialized with the wrong thread factory");
        }
    }

    private static void handleUsageError(String message) {
        handleUsageError(message, null);
    }

    private static void handleUsageError(String message, Throwable cause) {
        ExecutionExceptionHandler errorHandler = new ExecutionExceptionHandler();
        PrintWriter errStream = new PrintWriter(System.err, true);
        errorHandler.error(errStream, message, cause);
        System.exit(ExitCode.USAGE);
    }

    private static boolean isFastStart(List<String> cliArgs) {
        // 'start --optimized' should start the server without parsing CLI
        return cliArgs.size() == 2 && cliArgs.get(0).equals(Start.NAME) && cliArgs.stream().anyMatch(OPTIMIZED_BUILD_OPTION_LONG::equals);
    }

    public static void start(ExecutionExceptionHandler errorHandler, PrintWriter errStream, String[] args) {
        try {
            Quarkus.run(KeycloakMain.class, (exitCode, cause) -> {
                if (cause != null) {
                    errorHandler.error(errStream,
                            String.format("Failed to start server in (%s) mode", getKeycloakModeFromProfile(getProfileOrDefault("prod"))),
                            cause.getCause());
                }

                if (Environment.isDistribution()) {
                    // assume that it is running the distribution
                    // as we are replacing the default exit handler, we need to force exit
                    System.exit(exitCode);
                }
            }, args);
        } catch (Throwable cause) {
            errorHandler.error(errStream,
                    String.format("Unexpected error when starting the server in (%s) mode", getKeycloakModeFromProfile(getProfileOrDefault("prod"))),
                    cause.getCause());
            System.exit(1);
        }
    }

    /**
     * Should be called after the server is fully initialized
     */
    @Override
    public int run(String... args) throws Exception {
        if (isDevProfile()) {
            Logger.getLogger(KeycloakMain.class).warnf("Running the server in development mode. DO NOT use this configuration in production.");
        }

        int exitCode = ApplicationLifecycleManager.getExitCode();

        if (isTestLaunchMode() || isImportExportMode()) {
            // in test mode we exit immediately
            // we should be managing this behavior more dynamically depending on the tests requirements (short/long lived)
            Quarkus.asyncExit(exitCode);
        } else {
            Quarkus.waitForExit();
        }

        return exitCode;
    }

}
