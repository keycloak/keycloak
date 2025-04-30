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

package org.keycloak.quarkus.runtime.cli.command;

import java.util.EnumSet;

import org.keycloak.common.util.IoUtils;
import org.keycloak.config.BootstrapAdminOptions;
import org.keycloak.config.OptionCategory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.integration.jaxrs.QuarkusKeycloakApplication;
import org.keycloak.services.resources.KeycloakApplication;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = BootstrapAdminUser.NAME, header = BootstrapAdminUser.HEADER, description = "%n"
        + BootstrapAdminUser.HEADER)
public class BootstrapAdminUser extends AbstractNonServerCommand {

    public static final String NAME = "user";
    public static final String HEADER = "Add an admin user with a password";

    static class UsernameOptions {
        @Option(paramLabel = "username", names = { "--username" }, description = "Username of admin user, defaults to "
                + BootstrapAdminOptions.DEFAULT_TEMP_ADMIN_USERNAME)
        String username;

        @Option(paramLabel = "USERNAME", names = { "--username:env" }, description = "Environment variable name for the admin username")
        String usernameEnv;
    }

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    UsernameOptions usernameOptions;

    @Option(paramLabel = "PASSWORD", names = { "--password:env" }, description = "Environment variable name for the admin user password")
    String passwordEnv;

    String password;
    String username;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void doBeforeRun() {
        BootstrapAdmin bootstrap = spec.commandLine().getParent().getCommand();
        if (usernameOptions != null) {
            if (usernameOptions.usernameEnv != null) {
                username = getFromEnv(usernameOptions.usernameEnv);
            } else  {
                username = usernameOptions.username;
            }
        } else if (!bootstrap.noPrompt) {
            username = IoUtils.readLineFromConsole("username", BootstrapAdminOptions.DEFAULT_TEMP_ADMIN_USERNAME);
        }

        if (passwordEnv == null) {
            if (bootstrap.noPrompt) {
                throw new PropertyException("No password provided");
            }
            password = IoUtils.readPasswordFromConsole("password");
            String confirmPassword = IoUtils.readPasswordFromConsole("password again");
            if (!password.equals(confirmPassword)) {
                throw new PropertyException("Passwords do not match");
            }
            if (password.isBlank()) {
                throw new PropertyException("Password must not be blank");
            }
        } else {
            password = getFromEnv(passwordEnv);
        }
    }

    private String getFromEnv(String envVar) {
        String result = System.getenv(envVar);
        if (result == null) {
            throw new PropertyException(String.format("Environment variable %s not found", envVar));
        }
        return result;
    }

    @Override
    public void onStart(QuarkusKeycloakApplication application) {
        //BootstrapAdmin bootstrap = spec.commandLine().getParent().getCommand();
        KeycloakSessionFactory sessionFactory = KeycloakApplication.getSessionFactory();
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> application
                .createTemporaryMasterRealmAdminUser(username, password, /* bootstrap.expiration, */ session));
    }

    @Override
    protected EnumSet<OptionCategory> excludedCategories() {
        return EnumSet.of(OptionCategory.IMPORT, OptionCategory.EXPORT, OptionCategory.BOOTSTRAP_ADMIN);
    }

}
