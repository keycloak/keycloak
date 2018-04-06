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

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.util.ParseUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.keycloak.client.registration.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.registration.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.registration.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.registration.cli.util.ConfigUtil.getRegistrationToken;
import static org.keycloak.client.registration.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.registration.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.registration.cli.util.HttpUtil.doDelete;
import static org.keycloak.client.registration.cli.util.HttpUtil.urlencode;
import static org.keycloak.client.registration.cli.util.IoUtil.warnfErr;
import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.EOL;
import static org.keycloak.client.registration.cli.util.OsUtil.PROMPT;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "delete", description = "CLIENT [GLOBAL_OPTIONS]")
public class DeleteCmd extends AbstractAuthOptionsCmd {

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

            if (args.size() > 1) {
                throw new IllegalArgumentException("Invalid option: " + args.get(1));
            }

            String clientId = args.get(0);

            if (clientId.startsWith("-")) {
                warnfErr(ParseUtil.CLIENT_OPTION_WARN, clientId);
            }

            String regType = "default";

            ConfigData config = loadConfig();
            config = copyWithServerInfo(config);

            if (token == null) {
                // if registration access token is not set via -t, try use the one from configuration
                token = getRegistrationToken(config.sessionRealmConfigData(), clientId);
            }

            setupTruststore(config, commandInvocation);

            String auth = token;
            if (auth == null) {
                config = ensureAuthInfo(config, commandInvocation);
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
        return EOL + "Try '" + CMD + " help delete' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " delete CLIENT [ARGUMENTS]");
        out.println();
        out.println("Command to delete a specific client configuration. If registration access token is specified or is available in ");
        out.println("configuration file, then it is used. Otherwise, current active session is used.");
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
