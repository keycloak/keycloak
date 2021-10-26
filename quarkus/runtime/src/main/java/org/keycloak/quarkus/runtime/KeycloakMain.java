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

import static org.keycloak.quarkus.runtime.Environment.isDevMode;
import static org.keycloak.quarkus.runtime.cli.Picocli.error;
import static org.keycloak.quarkus.runtime.cli.Picocli.parseAndRun;
import static org.keycloak.quarkus.runtime.Environment.getProfileOrDefault;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.Quarkus;

import org.jboss.logging.Logger;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.common.Version;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * <p>The main entry point, responsible for initialize and run the CLI as well as start the server.
 */
@QuarkusMain(name = "keycloak")
public class KeycloakMain implements QuarkusApplication {

    private static final Logger LOGGER = Logger.getLogger(KeycloakMain.class);

    public static void main(String[] args) {
        System.setProperty("kc.version", Version.VERSION_KEYCLOAK);
        List<String> cliArgs = new ArrayList<>(Arrays.asList(args));
        System.setProperty(Environment.CLI_ARGS, Picocli.parseConfigArgs(cliArgs));

        if (cliArgs.isEmpty()) {
            // no arguments, just start the server without running picocli
            start(cliArgs, new PrintWriter(System.err));
            return;
        }

        // parse arguments and execute any of the configured commands
        parseAndRun(cliArgs);
    }

    public static void start(List<String> cliArgs, PrintWriter errorWriter) {
        try {
            Quarkus.run(KeycloakMain.class, (integer, cause) -> {
                if (cause != null) {
                    error(cliArgs, errorWriter,
                            String.format("Failed to start server using profile (%s)", getProfileOrDefault("prod")),
                            cause.getCause());
                }
            });
        } catch (Throwable cause) {
            error(cliArgs, errorWriter,
                    String.format("Unexpected error when starting the server using profile (%s)", getProfileOrDefault("prod")),
                    cause.getCause());
        }
    }

    /**
     * Should be called after the server is fully initialized
     */
    @Override
    public int run(String... args) throws Exception {
        if (isDevMode()) {
            LOGGER.warnf("Running the server in dev mode. DO NOT use this configuration in production.");
        }
        Quarkus.waitForExit();
        return ApplicationLifecycleManager.getExitCode();
    }
}
