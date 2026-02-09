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

package org.keycloak.client.registration.cli.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.registration.cli.CmdStdinContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.ConfigUtil.getRegistrationToken;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.cli.util.HttpUtil.doDelete;
import static org.keycloak.client.cli.util.HttpUtil.urlencode;
import static org.keycloak.client.cli.util.IoUtil.warnfErr;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "delete", description = "CLIENT [GLOBAL_OPTIONS]")
public class DeleteCmd extends AbstractAuthOptionsCmd {

    @Parameters(arity = "0..1")
    String clientId;

    @Override
    protected void process() {
        if (clientId == null) {
            throw new IllegalArgumentException("CLIENT not specified");
        }

        if (clientId.startsWith("-")) {
            warnfErr(CmdStdinContext.CLIENT_OPTION_WARN, clientId);
        }

        String regType = "default";

        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);

        if (externalToken == null) {
            // if registration access token is not set via -t, try use the one from configuration
            externalToken = getRegistrationToken(config.sessionRealmConfigData(), clientId);
        }

        setupTruststore(config);

        String auth = externalToken;
        if (auth == null) {
            config = ensureAuthInfo(config);
            config = copyWithServerInfo(config);
            if (credentialsAvailable(config)) {
                auth = ensureToken(config);
            }
        }

        auth = auth != null ? "Bearer " + auth : null;


        final String server = config.getServerUrl();
        final String realm = config.getRealm();

        doDelete(server + "/realms/" + realm + "/clients-registrations/" + regType + "/" + urlencode(clientId), auth);

        saveMergeConfig(cfg -> {
            cfg.ensureRealmConfigData(server, realm).getClients().remove(clientId);
        });
    }

    @Override
    protected boolean nothingToDo() {
        return super.nothingToDo() && clientId == null;
    }

    @Override
    protected String help() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " delete CLIENT [ARGUMENTS]");
        out.println();
        out.println("Command to delete a specific client configuration. If registration access token is specified or is available in ");
        out.println("configuration file, then it is used. Otherwise, current active session is used.");
        globalOptions(out);
        out.println("    CLIENT                ClientId of the client to delete");
        out.println("    -t, --token TOKEN     Use the specified Registration Access Token for authorization");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Delete a client:");
        out.println("  " + PROMPT + " " + CMD + " delete my_client");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
