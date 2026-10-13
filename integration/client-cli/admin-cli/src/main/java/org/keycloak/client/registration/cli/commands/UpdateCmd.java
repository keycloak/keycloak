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
import java.util.LinkedList;
import java.util.List;

import org.keycloak.client.cli.common.AttributeOperation;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.registration.cli.CmdStdinContext;
import org.keycloak.client.registration.cli.EndpointType;
import org.keycloak.client.registration.cli.EndpointTypeConverter;
import org.keycloak.client.registration.cli.ReflectionUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonParseException;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.cli.common.AttributeOperation.Type.DELETE;
import static org.keycloak.client.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.ConfigUtil.getRegistrationToken;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.cli.util.ConfigUtil.setRegistrationToken;
import static org.keycloak.client.cli.util.HttpUtil.APPLICATION_JSON;
import static org.keycloak.client.cli.util.HttpUtil.doGet;
import static org.keycloak.client.cli.util.HttpUtil.doPut;
import static org.keycloak.client.cli.util.HttpUtil.urlencode;
import static org.keycloak.client.cli.util.IoUtil.printOut;
import static org.keycloak.client.cli.util.IoUtil.readFully;
import static org.keycloak.client.cli.util.IoUtil.warnfErr;
import static org.keycloak.client.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.cli.util.ParseUtil.parseKeyVal;
import static org.keycloak.client.registration.cli.EndpointType.DEFAULT;
import static org.keycloak.client.registration.cli.EndpointType.OIDC;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@Command(name = "update", description = "CLIENT_ID [ARGUMENTS]")
public class UpdateCmd extends AbstractAuthOptionsCmd {

    @Option(names = {"-e", "--endpoint"}, description = "Endpoint type to use - one of: 'default', 'oidc'", converter = EndpointTypeConverter.class)
    private EndpointType regType = null;

    @Option(names = {"-f", "--file"}, description = "Use the file or standard input if '-' is specified")
    private String file = null;

    @Option(names = {"-m", "--merge"}, description = "Merge new values with existing configuration on the server")
    private boolean mergeMode = false;

    @Option(names = {"-o", "--output"}, description = "After update output the new client configuration")
    private boolean outputClient = false;

    @Option(names = {"-c", "--compressed"}, description = "Don't pretty print the output")
    private boolean compressed = false;

    @Parameters(arity = "0..1")
    String clientId;

    // to maintain relative positions of set and delete operations
    static class AttributeOperations {
        @Option(names = {"-s", "--set"}, required = true) String set;
        @Option(names = {"-d", "--delete"}, required = true) String delete;
    }

    @ArgGroup(exclusive = true, multiplicity = "0..*")
    List<AttributeOperations> rawAttributeOperations = new ArrayList<>();

    List<AttributeOperation> attrs = new LinkedList<>();

    @Override
    protected void processOptions() {
        super.processOptions();

        for (AttributeOperations entry : rawAttributeOperations) {
            if (entry.delete != null) {
                attrs.add(new AttributeOperation(DELETE, entry.delete));
            } else {
                String[] keyVal = parseKeyVal(entry.set);
                attrs.add(new AttributeOperation(SET, keyVal[0], keyVal[1]));
            }
        }
    }

    @Override
    protected void process() {
        if (clientId == null) {
            throw new IllegalArgumentException("CLIENT_ID not specified");
        }

        if (clientId.startsWith("-")) {
            warnfErr(CmdStdinContext.CLIENT_OPTION_WARN, clientId);
        }

        if (file == null && attrs.size() == 0) {
            throw new IllegalArgumentException("No file nor attribute values specified");
        }

        // We have several options for update:
        //
        // A) if a file is specified, then we can overwrite server state with that file
        //   (that's the normal flow - get and save locally, edit, post to server)
        //
        //   update my_client -f new_client.json
        //
        // B) if a file is specified, and overrides are specified, then we override the file values with those from command line
        //   (that allows us to have a local file as a template, it's also batch job friendly)
        //
        //   update my_client -s public=true -s enableDirectGrants=false -f new_client.json
        //
        // C) if no file is specified, then we can fetch the client definition from server, apply changes to it, and post it back
        //   (again a batch job friendly mode)
        //
        //   update my_client -s public=true -s enableDirectGrants=false
        //
        //   This is merge mode by default - if --merge is additionally specified, it is ignored
        //
        // D) if a file is specified, then we can merge the file with current state on the server
        //   (that is similar to previous mode except that the overrides are also taken from a file)
        //
        //   update my_client --merge -f new_client.json
        //   update my_client --merge -s public=true -s enableDirectGrants=false -f new_client.json
        //
        // We could also support environment variables in input file, and apply them before parsing it.
        //
        // One problem - what if it is SAML XML? No problem as we don't support update for SAML - only create.
        //
        if (file == null && attrs.size() > 0) {
            mergeMode = true;
        }

        CmdStdinContext ctx = new CmdStdinContext();
        if (file != null) {
            ctx = CmdStdinContext.parseFileOrStdin(file, regType);
            regType = ctx.getEndpointType();
        }

        if (regType == null) {
            regType = DEFAULT;
            ctx.setEndpointType(regType);
        } else if (regType != DEFAULT && regType != OIDC) {
            throw new RuntimeException("Update not supported for endpoint type: " + regType.getEndpoint());
        }

        // initialize config only after reading from stdin,
        // to allow proper operation when piping 'get' - which consumes the old
        // registration access token, and saves the new one to the config
        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);

        final String server = config.getServerUrl();
        final String realm = config.getRealm();

        if (externalToken == null) {
            // if registration access token is not set via --token, see if it's in the body of any input file
            // but first see if it's overridden by --set, or maybe deliberately muted via -d registrationAccessToken
            boolean processed = false;
            for (AttributeOperation op: attrs) {
                if ("registrationAccessToken".equals(op.getKey().toString())) {
                    processed = true;
                    if (op.getType() == AttributeOperation.Type.SET) {
                        externalToken = op.getValue();
                    }
                    // otherwise it's delete - meaning it should stay null
                    break;
                }
            }
            if (!processed) {
                externalToken = ctx.getRegistrationAccessToken();
            }
        }

        if (externalToken == null) {
            // if registration access token is not set, try use the one from configuration
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


        if (mergeMode) {
            InputStream response = doGet(server + "/realms/" + realm + "/clients-registrations/" + regType.getEndpoint() + "/" + urlencode(clientId),
                    APPLICATION_JSON, auth);

            String json = readFully(response);

            CmdStdinContext ctxremote = new CmdStdinContext();
            ctxremote.setContent(json);
            ctxremote.setEndpointType(regType);
            try {

                if (regType == DEFAULT) {
                    ctxremote.setClient(JsonSerialization.readValue(json, ClientRepresentation.class));
                    externalToken = ctxremote.getClient().getRegistrationAccessToken();
                } else if (regType == OIDC) {
                    ctxremote.setOidcClient(JsonSerialization.readValue(json, OIDCClientRepresentation.class));
                    externalToken = ctxremote.getOidcClient().getRegistrationAccessToken();
                }
            } catch (JsonParseException e) {
                throw new RuntimeException("Not a valid JSON document. " + e.getMessage(), e);
            } catch (IOException e) {
                throw new RuntimeException("Not a valid JSON document", e);
            }

            // we have to use registration access token retrieved from previous operation
            // that ensures optimistic locking semantics
            if (externalToken != null) {
                // we use auth with doPost later
                auth = "Bearer " + externalToken;

                String newToken = externalToken;
                String clientToUpdate = clientId;
                saveMergeConfig(cfg -> {
                    setRegistrationToken(cfg.ensureRealmConfigData(server, realm), clientToUpdate, newToken);
                });
            }

            // merge local representation over remote one
            if (ctx.getClient() != null) {
                ReflectionUtil.merge(ctx.getClient(), ctxremote.getClient());
            } else if (ctx.getOidcClient() != null) {
                ReflectionUtil.merge(ctx.getOidcClient(), ctxremote.getOidcClient());
            }
            ctx = ctxremote;
        }

        if (attrs.size() > 0) {
            ctx = CmdStdinContext.mergeAttributes(ctx, attrs);
        }

        // now update
        InputStream response = doPut(server + "/realms/" + realm + "/clients-registrations/" + regType.getEndpoint() + "/" + urlencode(clientId),
                APPLICATION_JSON, APPLICATION_JSON, ctx.getContent(), auth);
        try {
            if (regType == DEFAULT) {
                ClientRepresentation clirep = JsonSerialization.readValue(response, ClientRepresentation.class);
                outputResult(clirep);
                externalToken = clirep.getRegistrationAccessToken();
            } else if (regType == OIDC) {
                OIDCClientRepresentation clirep = JsonSerialization.readValue(response, OIDCClientRepresentation.class);
                outputResult(clirep);
                externalToken = clirep.getRegistrationAccessToken();
            }

            String newToken = externalToken;
            String clientToUpdate = clientId;
            saveMergeConfig(cfg -> {
                setRegistrationToken(cfg.ensureRealmConfigData(server, realm), clientToUpdate, newToken);
            });

        } catch (IOException e) {
            throw new RuntimeException("Failed to process HTTP response", e);
        }
    }

    private void outputResult(Object result) throws IOException {
        if (outputClient) {
            if (compressed) {
                printOut(JsonSerialization.writeValueAsString(result));
            } else {
                printOut(JsonSerialization.writeValueAsPrettyString(result));
            }
        }
    }

    @Override
    protected boolean nothingToDo() {
        return super.nothingToDo() && regType == null && file == null && rawAttributeOperations.isEmpty() && clientId == null;
    }

    @Override
    protected String help() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " update CLIENT [ARGUMENTS]");
        out.println();
        out.println("Command to update an existing client configuration. If registration access token is specified it is used.");
        out.println("Otherwise, if 'registrationAccessToken' attribute is set, that is used. Otherwise, if registration access");
        out.println("token is available in configuration file, we use that. Finally, if it's not available anywhere, the current ");
        out.println("active session is used.");
        globalOptions(out);
        out.println("  Command specific options:");
        out.println("    CLIENT                ClientId of the client to update");
        out.println("    -t, --token TOKEN     Use the specified Registration Access Token for authorization");
        out.println("    -s, --set KEY=VALUE   Set specific attribute to a specified value");
        out.println("              KEY+=VALUE  Add item to an array");
        out.println("    -d, --delete NAME     Delete the specific attribute, or array item");
        out.println("    -e, --endpoint TYPE   Endpoint type to use - one of: 'default', 'oidc'");
        out.println("    -f, --file FILENAME   Use the file or standard input if '-' is specified");
        out.println("    -m, --merge           Merge new values with existing configuration on the server");
        out.println("                          Merge is automatically enabled unless --file is specified");
        out.println("    -o, --output          After update output the new client configuration");
        out.println("    -c, --compressed      Don't pretty print the output");
        out.println();
        out.println();
        out.println("Nested attributes are supported by using '.' to separate components of a KEY. Optionally, the KEY components ");
        out.println("can be quoted with double quotes - e.g. my_client.attributes.\"external.user.id\". If VALUE starts with [ and ");
        out.println("ends with ] the attribute will be set as a JSON array. If VALUE starts with { and ends with } the attribute ");
        out.println("will be set as a JSON object. If KEY ends with an array index - e.g. clients[3]=VALUE - then the specified item");
        out.println("of the array is updated. If KEY+=VALUE syntax is used, then KEY is assumed to be an array, and another item is");
        out.println("added to it.");
        out.println();
        out.println("Attributes can also be deleted. If KEY ends with an array index, then the targeted item of an array is removed");
        out.println("and the following items are shifted.");
        out.println();
        out.println("Merged mode fetches current configuration from the server, applies attribute changes to it, and sends it");
        out.println("back to the server, overwriting existing configuration there. If available, Registration Access Token is used ");
        out.println("for authorization when doing changes. Otherwise current session's authorization is used in which case user needs");
        out.println("manage-clients permission for update to work.");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Update a client by fetching current configuration from server, and applying specified changes.");
        out.println("  " + PROMPT + " " + CMD + " update my_client -s enabled=true -s 'redirectUris=[\"http://localhost:8080/myapp/*\"]'");
        out.println();
        out.println("Update a client by overwriting existing configuration on the server with a new one:");
        out.println("  " + PROMPT + " " + CMD + " update my_client -f new_my_client.json");
        out.println();
        out.println("Update a client by overwriting existing configuration using local file as a template:");
        out.println("  " + PROMPT + " " + CMD + " update my_client -f new_my_client.json -s enabled=true");
        out.println();
        out.println("Update client by fetching current configuration from server and merging with specified changes:");
        out.println("  " + PROMPT + " " + CMD + " update my_client -f new_my_client.json -s enabled=true --merge");
        out.println();
        out.println("Update a client using 'oidc' endpoint:");
        out.println("  " + PROMPT + " " + CMD + " update my_client -e oidc -s 'redirect_uris=[\"http://localhost:8080/myapp/*\"]'");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
