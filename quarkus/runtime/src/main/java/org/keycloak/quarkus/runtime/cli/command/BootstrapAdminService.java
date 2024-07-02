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

import org.keycloak.common.util.IoUtils;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.integration.jaxrs.QuarkusKeycloakApplication;
import org.keycloak.services.managers.ApplianceBootstrap;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = BootstrapAdminService.NAME, header = BootstrapAdminService.HEADER, description = "%n"
        + BootstrapAdminService.HEADER)
public class BootstrapAdminService extends AbstractNonServerCommand {

    public static final String NAME = "service";
    public static final String HEADER = "Add an admin service account";

    static class ClientIdOptions {
        @Option(names = { "--client-id" }, description = "Client id, defaults to "
                + ApplianceBootstrap.DEFAULT_TEMP_ADMIN_SERVICE)
        String clientId;

        @Option(names = { "--client-id:env" }, description = "Environment variable name for the client id")
        String cliendIdEnv;
    }

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    ClientIdOptions clientIdOptions;

    @Option(names = { "--client-secret:env" }, description = "Environment variable name for the client secret")
    String clientSecretEnv;

    String clientSecret;
    String clientId;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void doBeforeRun() {
        BootstrapAdmin bootstrap = spec.commandLine().getParent().getCommand();
        if (clientIdOptions != null) {
            if (clientIdOptions.cliendIdEnv != null) {
                clientId = getFromEnv(clientIdOptions.cliendIdEnv);
            } else  {
                clientId = clientIdOptions.clientId;
            }
        } else if (!bootstrap.noPrompt) {
            clientId = IoUtils.readLineFromConsole("client id", ApplianceBootstrap.DEFAULT_TEMP_ADMIN_SERVICE);
        }

        if (clientSecretEnv == null) {
            if (bootstrap.noPrompt) {
                throw new PropertyException("No client secret provided");
            }
            clientSecret = IoUtils.readPasswordFromConsole("client secret");
            String confirmClientSecret = IoUtils.readPasswordFromConsole("client secret again");
            if (!clientSecret.equals(confirmClientSecret)) {
                throw new PropertyException("Client secrets do not match");
            }
        } else {
            clientSecret = getFromEnv(clientSecretEnv);
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
        application.createTemporaryMasterRealmAdminService(clientId, clientSecret, /*bootstrap.expiration,*/ null);
    }

}
