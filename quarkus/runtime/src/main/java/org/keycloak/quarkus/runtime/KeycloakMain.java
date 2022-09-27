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
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.*;
import static org.keycloak.quarkus.runtime.cli.command.Start.isDevProfileNotAllowed;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.Quarkus;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.common.Version;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * <p>The main entry point, responsible for initialize and run the CLI as well as start the server.
 */
@QuarkusMain(name = "keycloak")
@ApplicationScoped
public class KeycloakMain implements QuarkusApplication {

    private static final String KEYCLOAK_ADMIN_ENV_VAR = "KEYCLOAK_ADMIN";
    private static final String KEYCLOAK_ADMIN_PASSWORD_ENV_VAR = "KEYCLOAK_ADMIN_PASSWORD";

    public static void main(String[] args) {
        System.setProperty("kc.version", Version.VERSION_KEYCLOAK);
        List<String> cliArgs = Picocli.parseArgs(args);

        if (cliArgs.isEmpty()) {
            cliArgs = new ArrayList<>(cliArgs);
            // default to show help message
            cliArgs.add("-h");
        } else if (isFastStart(cliArgs)) {
            // fast path for starting the server without bootstrapping CLI
            ExecutionExceptionHandler errorHandler = new ExecutionExceptionHandler();
            PrintWriter errStream = new PrintWriter(System.err, true);

            if (isDevProfileNotAllowed(Arrays.asList(args))) {
                errorHandler.error(errStream, Messages.devProfileNotAllowedError(Start.NAME), null);
                return;
            }

            start(errorHandler, errStream, args);

            return;
        }

        // parse arguments and execute any of the configured commands
        parseAndRun(cliArgs);
    }

    private static boolean isFastStart(List<String> cliArgs) {
        // 'start --optimized' should start the server without parsing CLI
        return cliArgs.size() == 2 && cliArgs.get(0).equals(Start.NAME) && cliArgs.stream().anyMatch(OPTIMIZED_BUILD_OPTION_LONG::equals);
    }

    public static void start(ExecutionExceptionHandler errorHandler, PrintWriter errStream, String[] args) {
        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(new KeycloakClassLoader());

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
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    /**
     * Should be called after the server is fully initialized
     */
    @Override
    public int run(String... args) throws Exception {
        if (!isImportExportMode()) {
            createAdminUser();
        }

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

    private void createAdminUser() {
        String adminUserName = System.getenv(KEYCLOAK_ADMIN_ENV_VAR);
        String adminPassword = System.getenv(KEYCLOAK_ADMIN_PASSWORD_ENV_VAR);

        if ((adminUserName == null || adminUserName.trim().length() == 0)
                || (adminPassword == null || adminPassword.trim().length() == 0)) {
            return;
        }

        KeycloakSessionFactory sessionFactory = KeycloakApplication.getSessionFactory();
        KeycloakSession session = sessionFactory.create();
        KeycloakTransactionManager transaction = session.getTransactionManager();

        try {
            transaction.begin();

            new ApplianceBootstrap(session).createMasterRealmUser(adminUserName, adminPassword);
            ServicesLogger.LOGGER.addUserSuccess(adminUserName, Config.getAdminRealm());

            transaction.commit();
        } catch (IllegalStateException e) {
            session.getTransactionManager().rollback();
            ServicesLogger.LOGGER.addUserFailedUserExists(adminUserName, Config.getAdminRealm());
        } catch (Throwable t) {
            session.getTransactionManager().rollback();
            ServicesLogger.LOGGER.addUserFailed(t, adminUserName, Config.getAdminRealm());
        } finally {
            session.close();
        }
    }
}
