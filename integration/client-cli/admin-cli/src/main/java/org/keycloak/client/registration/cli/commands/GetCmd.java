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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.registration.cli.CmdStdinContext;
import org.keycloak.client.registration.cli.EndpointType;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JsonSerialization;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.ConfigUtil.getRegistrationToken;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.cli.util.ConfigUtil.setRegistrationToken;
import static org.keycloak.client.cli.util.HttpUtil.APPLICATION_JSON;
import static org.keycloak.client.cli.util.HttpUtil.doGet;
import static org.keycloak.client.cli.util.HttpUtil.urlencode;
import static org.keycloak.client.cli.util.IoUtil.printOut;
import static org.keycloak.client.cli.util.IoUtil.readFully;
import static org.keycloak.client.cli.util.IoUtil.warnfErr;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;

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
            warnfErr(CmdStdinContext.CLIENT_OPTION_WARN, clientId);
        }

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
        return super.nothingToDo() && endpoint == null && clientId == null;
    }

    @Override
    protected String help() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " get CLIENT [ARGUMENTS]");
        out.println();
        out.println("Command to retrieve a client configuration description for a specified client. If registration access token");
        out.println("is specified or is available in configuration file, then it is used. Otherwise, current active session is used.");
        globalOptions(out);
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
