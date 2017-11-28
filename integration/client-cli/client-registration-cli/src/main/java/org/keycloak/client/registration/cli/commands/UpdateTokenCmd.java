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

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.util.ParseUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.keycloak.client.registration.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.registration.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.registration.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.registration.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.registration.cli.util.ConfigUtil.setRegistrationToken;
import static org.keycloak.client.registration.cli.util.HttpUtil.APPLICATION_JSON;
import static org.keycloak.client.registration.cli.util.HttpUtil.doGet;
import static org.keycloak.client.registration.cli.util.HttpUtil.doPost;
import static org.keycloak.client.registration.cli.util.IoUtil.printOut;
import static org.keycloak.client.registration.cli.util.IoUtil.warnfOut;
import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.EOL;
import static org.keycloak.client.registration.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "update-token", description = "CLIENT [ARGUMENTS]")
public class UpdateTokenCmd extends AbstractAuthOptionsCmd {

    @Arguments
    private List<String> args;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        try {
            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            processGlobalOptions();

            if (args == null || args.isEmpty()) {
                throw new IllegalArgumentException("CLIENT not specified");
            }

            String clientId = args.get(0);

            if (clientId.startsWith("-")) {
                warnfOut(ParseUtil.CLIENT_OPTION_WARN, clientId);
            }

            ConfigData config = loadConfig();
            config = copyWithServerInfo(config);
            setupTruststore(config, commandInvocation);

            config = ensureAuthInfo(config, commandInvocation);
            String auth = ensureToken(config);

            String cid = null;

            final String server = config.getServerUrl();
            final String realm = config.getRealm();

            // first we need to get id of the client with client_id == clientId
            InputStream response = doGet(server + "/admin/realms/" + realm + "/clients", APPLICATION_JSON, "Bearer " + auth);
            try {
                List<ClientRepresentation> clients = JsonSerialization.readValue(response, new TypeReference<List<ClientRepresentation>>() {});
                for (ClientRepresentation client: clients) {
                    if (clientId.equals(client.getClientId())) {
                        cid = client.getId();
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to process response from server", e);
            }

            if (cid == null) {
                throw new RuntimeException("No client found for: " + clientId);
            }

            response = doPost(server + "/admin/realms/" + realm + "/clients/" + cid + "/registration-access-token",
                    APPLICATION_JSON, APPLICATION_JSON, null, "Bearer " + auth);

            try {
                ClientRepresentation client = JsonSerialization.readValue(response, ClientRepresentation.class);

                if (noconfig) {
                    // output to stdout
                    printOut(client.getRegistrationAccessToken());
                } else {
                    saveMergeConfig(cfg -> {
                        setRegistrationToken(cfg.ensureRealmConfigData(server, realm), client.getClientId(), client.getRegistrationAccessToken());
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to process response from server", e);
            }

            //System.out.println("Token updated for client " + clientId);
            return CommandResult.SUCCESS;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
    }

    @Override
    protected boolean nothingToDo() {
        return noOptions() && (args == null || args.size() == 0);
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help update-token' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " update-token CLIENT [ARGUMENTS]");
        out.println();
        out.println("Command to reissue, and set a new registration access token if an old one is lost or becomes invalid.");
        out.println("It requires an authenticated session using an account with administrator priviliges.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                    Print full stack trace when exiting with error");
        out.println("    --config              Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println("    --no-config           Don't use config file - no authentication info is loaded or saved");
        out.println("    --truststore PATH     Path to a truststore containing trusted certificates");
        out.println("    --trustpass PASSWORD  Truststore password (prompted for if not specified and --truststore is used)");
        out.println("    CREDENTIALS OPTIONS   Same set of options as accepted by '" + CMD + " config credentials' in order to establish");
        out.println("                          an authenticated sessions. In combination with --no-config option this allows transient");
        out.println("                          (on-the-fly) authentication to be performed which leaves no tokens in config file.");
        out.println();
        out.println("  Command specific options:");
        out.println("    CLIENT                ClientId of the client to reissue a new Registration Access Token for");
        out.println("                          The new token is saved to a config file or printed to stdout if --no-config");
        out.println("                          (on-the-fly) authentication is used");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Request a new Registration Access Token from the server using current authenticated session:");
        out.println("  " + PROMPT + " " + CMD + " update-token my_client");
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
