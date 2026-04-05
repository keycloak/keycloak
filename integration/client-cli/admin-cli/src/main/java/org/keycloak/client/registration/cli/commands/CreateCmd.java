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
import java.util.ArrayList;
import java.util.List;

import org.keycloak.client.cli.common.AttributeOperation;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.client.registration.cli.CmdStdinContext;
import org.keycloak.client.registration.cli.EndpointType;
import org.keycloak.client.registration.cli.EndpointTypeConverter;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static org.keycloak.client.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.cli.util.ConfigUtil.setRegistrationToken;
import static org.keycloak.client.cli.util.HttpUtil.doPost;
import static org.keycloak.client.cli.util.IoUtil.printErr;
import static org.keycloak.client.cli.util.IoUtil.printOut;
import static org.keycloak.client.cli.util.IoUtil.readFully;
import static org.keycloak.client.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.cli.util.ParseUtil.parseKeyVal;
import static org.keycloak.client.registration.cli.EndpointType.DEFAULT;
import static org.keycloak.client.registration.cli.EndpointType.OIDC;
import static org.keycloak.client.registration.cli.EndpointType.SAML2;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;
import static org.keycloak.common.util.IoUtils.readPasswordFromConsole;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "create", description = "[ARGUMENTS]")
public class CreateCmd extends AbstractAuthOptionsCmd {

    @Option(names = {"-i", "--clientId"}, description = "After creation only print clientId to standard output")
    protected boolean returnClientId = false;

    @Option(names = {"-e", "--endpoint"}, description = "Endpoint type / document format to use - one of: 'default', 'oidc', 'saml2'", converter = EndpointTypeConverter.class)
    protected EndpointType regType;

    @Option(names = {"-f", "--file"}, description = "Read object from file or standard input if FILENAME is set to '-'")
    protected String file;

    @Option(names = {"-o", "--output"}, description = "After creation output the new client configuration to standard output")
    protected boolean outputClient = false;

    @Option(names = {"-c", "--compressed"}, description = "Don't pretty print the output")
    protected boolean compressed = false;

    @Option(names = {"-s", "--set"}, description = "Set a specific attribute NAME to a specified value VALUE")
    List<String> rawSets = new ArrayList<>();

    List<AttributeOperation> attrs = new ArrayList<>();

    @Override
    protected void processOptions() {
        super.processOptions();

        for (String set : rawSets) {
            String[] keyVal = parseKeyVal(set);
            attrs.add(new AttributeOperation(SET, keyVal[0], keyVal[1]));
        }
    }

    @Override
    protected void process() {
        if (file == null && attrs.size() == 0) {
            throw new IllegalArgumentException("No file nor attribute values specified");
        }

        if (outputClient && returnClientId) {
            throw new IllegalArgumentException("Options -o and -i are mutually exclusive");
        }

        // if --token is specified read it
        if ("-".equals(externalToken)) {
            externalToken = readPasswordFromConsole("Initial Access Token");
        }

        CmdStdinContext ctx = new CmdStdinContext();
        if (file != null) {
            ctx = CmdStdinContext.parseFileOrStdin(file, regType);
        }

        if (ctx.getEndpointType() == null) {
            regType = regType != null ? regType : DEFAULT;
            ctx.setEndpointType(regType);
        } else if (regType != null && ctx.getEndpointType() != regType) {
            throw new RuntimeException("Requested endpoint type not compatible with detected configuration format: " + ctx.getEndpointType());
        }

        if (attrs.size() > 0) {
            ctx = CmdStdinContext.mergeAttributes(ctx, attrs);
        }

        String contentType = EndpointType.getExpectedContentType(ctx.getEndpointType());

        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);

        if (externalToken == null) {
            // if initial token is not set, try use the one from configuration
            externalToken = config.sessionRealmConfigData().getInitialToken();
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

        InputStream response = doPost(server + "/realms/" + realm + "/clients-registrations/" + ctx.getEndpointType().getEndpoint(),
                contentType, HttpUtil.APPLICATION_JSON, ctx.getContent(), auth);

        try {
            if (ctx.getEndpointType() == DEFAULT || ctx.getEndpointType() == SAML2) {
                ClientRepresentation client = JsonSerialization.readValue(response, ClientRepresentation.class);
                outputResult(client.getClientId(), client);

                saveMergeConfig(cfg -> {
                    setRegistrationToken(cfg.ensureRealmConfigData(server, realm), client.getClientId(), client.getRegistrationAccessToken());
                });
            } else if (ctx.getEndpointType() == OIDC) {
                OIDCClientRepresentation client = JsonSerialization.readValue(response, OIDCClientRepresentation.class);
                outputResult(client.getClientId(), client);

                saveMergeConfig(cfg -> {
                    setRegistrationToken(cfg.ensureRealmConfigData(server, realm), client.getClientId(), client.getRegistrationAccessToken());
                });
            } else {
                printOut("Response from server: " + readFully(response));
            }
        } catch (UnrecognizedPropertyException e) {
            throw new RuntimeException("Failed to process HTTP response - " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process HTTP response", e);
        }
    }

    private void outputResult(String clientId, Object result) throws IOException {
        if (returnClientId) {
            printOut(clientId);
        } else if (outputClient) {
            if (compressed) {
                printOut(JsonSerialization.writeValueAsString(result));
            } else {
                printOut(JsonSerialization.writeValueAsPrettyString(result));
            }
        } else {
            printErr("Registered new client with client_id '" + clientId + "'");
        }
    }

    @Override
    protected boolean nothingToDo() {
        return super.nothingToDo() && regType == null && file == null && rawSets.isEmpty();
    }

    @Override
    protected String help() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " create [ARGUMENTS]");
        out.println();
        out.println("Command to create new client configurations on the server. If Initial Access Token is specified (-t TOKEN)");
        out.println("or has previously been set for the server, and realm in the configuration ('" + CMD + " config initial-token'),");
        out.println("then that will be used, otherwise session access / refresh tokens will be used.");
        globalOptions(out);
        out.println("    -t, --token TOKEN     Use the specified Initial Access Token for authorization or read it from standard input ");
        out.println("                          if '-' is specified. This overrides any token set by '" + CMD + " config initial-token'.");
        out.println("                          If not specified, session credentials are used - see: CREDENTIALS OPTIONS.");
        out.println("    -e, --endpoint TYPE   Endpoint type / document format to use - one of: 'default', 'oidc', 'saml2'.");
        out.println("                          If not specified, the format is deduced from input file or falls back to 'default'.");
        out.println("    -s, --set NAME=VALUE  Set a specific attribute NAME to a specified value VALUE");
        out.println("    -f, --file FILENAME   Read object from file or standard input if FILENAME is set to '-'");
        out.println("    -o, --output          After creation output the new client configuration to standard output");
        out.println("    -c, --compressed      Don't pretty print the output");
        out.println("    -i, --clientId        After creation only print clientId to standard output");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Create a new client using configuration read from standard input:");
        if (OS_ARCH.isWindows()) {
            out.println("  " + PROMPT + " echo { \"clientId\": \"my_client\" } | " + CMD + " create -f -");
        } else {
            out.println("  " + PROMPT + " " + CMD + " create -f - << EOF");
            out.println("  {");
            out.println("    \"clientId\": \"my_client\"");
            out.println("  }");
            out.println("  EOF");
        }
        out.println();
        out.println("Since we didn't specify an endpoint type it will be deduced from configuration format.");
        out.println("Supported formats include Keycloak default format, OIDC format, and SAML SP Metadata.");
        out.println();
        out.println("Creating a client using file as a template, and overriding some attributes:");
        out.println("  " + PROMPT + " " + CMD + " create -f my_client.json -s clientId=my_client2 -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]'");
        out.println();
        out.println("Creating a client using an Initial Access Token - you'll be prompted for a token:");
        out.println("  " + PROMPT + " " + CMD + " create -s clientId=my_client2 -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]' -t -");
        out.println();
        out.println("Creating a client using 'oidc' endpoint. Without setting endpoint type here it would be 'default':");
        out.println("  " + PROMPT + " " + CMD + " create -e oidc -s 'redirect_uris=[\"http://localhost:8980/myapp/*\"]'");
        out.println();
        out.println("Creating a client using 'saml2' endpoint. In this case setting endpoint type is redundant since it is deduced ");
        out.println("from file content:");
        out.println("  " + PROMPT + " " + CMD + " create -e saml2 -f saml-sp-metadata.xml");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }

}
