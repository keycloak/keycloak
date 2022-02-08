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
import static org.keycloak.quarkus.runtime.Environment.isTestLaunchMode;
import static org.keycloak.quarkus.runtime.cli.Picocli.parseAndRun;
import static org.keycloak.quarkus.runtime.cli.command.Start.isDevProfileNotAllowed;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.Quarkus;

import org.jboss.logging.Logger;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;
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

    private static final Logger LOGGER = Logger.getLogger(KeycloakMain.class);

    public static void main(String[] args) {
        System.setProperty("kc.version", Version.VERSION_KEYCLOAK);
        List<String> cliArgs = Picocli.parseArgs(args);

        if (cliArgs.isEmpty()) {
            cliArgs = new ArrayList<>(cliArgs);
            // default to show help message
            cliArgs.add("-h");
        } else if (cliArgs.contains(Start.NAME) && cliArgs.size() == 1) {
            // fast path for starting the server without bootstrapping CLI
            ExecutionExceptionHandler errorHandler = new ExecutionExceptionHandler();
            PrintWriter errStream = new PrintWriter(System.err, true);

            if (isDevProfileNotAllowed(Arrays.asList(args))) {
                errorHandler.error(errStream, Messages.devProfileNotAllowedError(Start.NAME), null);
                return;
            }

            start(errorHandler, errStream);

            return;
        }

        // parse arguments and execute any of the configured commands
        parseAndRun(cliArgs);
    }

    public static void start(ExecutionExceptionHandler errorHandler, PrintWriter errStream) {
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
            });
        } catch (Throwable cause) {
            errorHandler.error(errStream,
                    String.format("Unexpected error when starting the server in (%s) mode", getKeycloakModeFromProfile(getProfileOrDefault("prod"))),
                    cause.getCause());
        }
    }

    /**
     * Should be called after the server is fully initialized
     */
    @Override
    public int run(String... args) throws Exception {
        if (isDevProfile()) {
            LOGGER.warnf("Running the server in development mode. DO NOT use this configuration in production.");
        }

        int exitCode = ApplicationLifecycleManager.getExitCode();

        if (isTestLaunchMode()) {
            // in test mode we exit immediately
            // we should be managing this behavior more dynamically depending on the tests requirements (short/long lived)
            Quarkus.asyncExit(exitCode);
        } else {
            Quarkus.waitForExit();
        }

        return exitCode;
    }
}
