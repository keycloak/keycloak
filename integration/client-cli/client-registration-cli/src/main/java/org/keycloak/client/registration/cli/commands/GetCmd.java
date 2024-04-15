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

import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.common.EndpointType;
import org.keycloak.client.registration.cli.util.ParseUtil;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.registration.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.registration.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.registration.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.registration.cli.util.ConfigUtil.getRegistrationToken;
import static org.keycloak.client.registration.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.registration.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.registration.cli.util.ConfigUtil.setRegistrationToken;
import static org.keycloak.client.registration.cli.util.HttpUtil.APPLICATION_JSON;
import static org.keycloak.client.registration.cli.util.HttpUtil.doGet;
import static org.keycloak.client.registration.cli.util.HttpUtil.urlencode;
import static org.keycloak.client.registration.cli.util.IoUtil.warnfErr;
import static org.keycloak.client.registration.cli.util.IoUtil.printOut;
import static org.keycloak.client.registration.cli.util.IoUtil.readFully;
import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.PROMPT;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "get", description = "[ARGUMENTS]")
public class GetCmd extends AbstractAuthOptionsCmd {

    @Option(names = {"-c", "--compressed"}, description = "Print full stack trace when exiting with error")
    private boolean compressed = false;

    @Option(names = {"-e", "--endpoint"}, description = "Endpoint type to use")
    private String endpoint;

    @Parameters(arity = "0..1")
    String clientId;

    @Override
    protected void process() {
        if (clientId == null) {
            throw new IllegalArgumentException("CLIENT not specified");
        }

        EndpointType regType = endpoint != null ? EndpointType.of(endpoint) : EndpointType.DEFAULT;


        if (clientId.startsWith("-")) {
            warnfErr(ParseUtil.CLIENT_OPTION_WARN, clientId);
        }

        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);

        if (token == null) {
            // if registration access token is not set via -t, try use the one from configuration
            token = getRegistrationToken(config.sessionRealmConfigData(), clientId);
        }

        setupTruststore(config);

        String auth = token;
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

        InputStream response = doGet(server + "/realms/" + realm + "/clients-registrations/" + regType.getEndpoint() + "/" + urlencode(clientId),
                APPLICATION_JSON, auth);

        try {
            String json = readFully(response);
            Object result = null;

            switch (regType) {
                case DEFAULT: {
                    ClientRepresentation client = JsonSerialization.readValue(json, ClientRepresentation.class);
                    result = client;

                    saveMergeConfig(cfg -> {
                        setRegistrationToken(cfg.ensureRealmConfigData(server, realm), client.getClientId(), client.getRegistrationAccessToken());
                    });
                    break;
                }
                case OIDC: {
                    OIDCClientRepresentation client = JsonSerialization.readValue(json, OIDCClientRepresentation.class);
                    result = client;

                    saveMergeConfig(cfg -> {
                        setRegistrationToken(cfg.ensureRealmConfigData(server, realm), client.getClientId(), client.getRegistrationAccessToken());
                    });
                    break;
                }
                case INSTALL: {
                    result = JsonSerialization.readValue(json, AdapterConfig.class);
                    break;
                }
                case SAML2: {
                    break;
                }
                default: {
                    throw new RuntimeException("Unexpected type: " + regType);
                }
            }

            if (!compressed && result != null) {
                json = JsonSerialization.writeValueAsPrettyString(result);
            }

            printOut(json);

        //} catch (UnrecognizedPropertyException e) {
        //    throw new RuntimeException("Failed to parse returned JSON - " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process HTTP response", e);
        }
    }

    @Override
    protected boolean nothingToDo() {
        return noOptions() && endpoint == null && clientId == null;
    }

    @Override
    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " get CLIENT [ARGUMENTS]");
        out.println();
        out.println("Command to retrieve a client configuration description for a specified client. If registration access token");
        out.println("is specified or is available in configuration file, then it is used. Otherwise, current active session is used.");
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
        out.println("    CLIENT                ClientId of the client to display");
        out.println("    -t, --token TOKEN     Use the specified Registration Access Token for authorization");
        out.println("    -c, --compressed      Don't pretty print the output");
        out.println("    -e, --endpoint TYPE   Endpoint type to use - one of: 'default', 'oidc', 'install'");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Get configuration in default format:");
        out.println("  " + PROMPT + " " + CMD + " get my_client");
        out.println();
        out.println("Get configuration in OIDC format:");
        out.println("  " + PROMPT + " " + CMD + " get my_client -e oidc");
        out.println();
        out.println("Get adapter configuration for the client:");
        out.println("  " + PROMPT + " " + CMD + " get my_client -e install");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
