/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.client.admin.cli.commands;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.cli.common.BaseConfigCredentialsCmd;
import org.keycloak.client.cli.config.ConfigData;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import static java.lang.System.currentTimeMillis;

import static org.keycloak.client.cli.util.AuthUtil.AUTH_BUFFER_TIME;
import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.cli.util.IoUtil.printOut;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = ConfigCredentialsCmd.NAME, description = "--server SERVER_URL --realm REALM [ARGUMENTS]")
public class ConfigCredentialsCmd extends BaseConfigCredentialsCmd {

    static final String NAME = "credentials";

    public ConfigCredentialsCmd() {
        super(KcAdmMain.COMMAND_STATE);
    }

    @CommandLine.Option(names = "--status", description = "Validity of the connection with server")
    boolean status;

    @Override
    protected boolean nothingToDo() {
        return super.nothingToDo() && !status;
    }

    @Override
    public void process() {
        if (status) {
            ConfigData config = loadConfig();
            long now = currentTimeMillis();
            if (credentialsAvailable(config) && now + AUTH_BUFFER_TIME < config.sessionRealmConfigData().getExpiresAt()) {
                printOut("Logged in (server: " + config.getServerUrl() + ", realm: " + config.getRealm() + ", expired: false, timeToExpiry: " + (config.sessionRealmConfigData().getExpiresAt() - now) / 1000 + "s from now)");
            } else {
                printOut("You are not logged in");
            }
        } else {
            super.process();
        }
    }

    String getPassword() {
        return this.password;
    }
}
