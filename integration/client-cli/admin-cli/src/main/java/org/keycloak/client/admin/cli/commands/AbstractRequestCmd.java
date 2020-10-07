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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.common.AttributeOperation;
import org.keycloak.client.admin.cli.common.CmdStdinContext;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.util.AccessibleBufferOutputStream;
import org.keycloak.client.admin.cli.util.Header;
import org.keycloak.client.admin.cli.util.Headers;
import org.keycloak.client.admin.cli.util.HeadersBody;
import org.keycloak.client.admin.cli.util.HeadersBodyStatus;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.client.admin.cli.util.OutputFormat;
import org.keycloak.client.admin.cli.util.ReflectionUtil;
import org.keycloak.client.admin.cli.util.ReturnFields;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.keycloak.client.admin.cli.common.AttributeOperation.Type.DELETE;
import static org.keycloak.client.admin.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.HttpUtil.checkSuccess;
import static org.keycloak.client.admin.cli.util.HttpUtil.composeResourceUrl;
import static org.keycloak.client.admin.cli.util.HttpUtil.doGet;
import static org.keycloak.client.admin.cli.util.IoUtil.copyStream;
import static org.keycloak.client.admin.cli.util.IoUtil.printErr;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;
import static org.keycloak.client.admin.cli.util.OutputUtil.MAPPER;
import static org.keycloak.client.admin.cli.util.OutputUtil.printAsCsv;
import static org.keycloak.client.admin.cli.util.ParseUtil.mergeAttributes;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseFileOrStdin;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractRequestCmd extends AbstractAuthOptionsCmd {

    String file;

    String body;

    String fields;

    boolean printHeaders;

    boolean returnId;

    boolean outputResult;

    boolean compressed;

    boolean unquoted;

    boolean mergeMode;

    boolean noMerge;

    Integer offset;

    Integer limit;

    String format = "json";

    OutputFormat outputFormat;

    String httpVerb;

    Headers headers = new Headers();

    List<AttributeOperation> attrs = new LinkedList<>();

    Map<String, String> filter = new HashMap<>();

    String url = null;


    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            initOptions();

            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            processGlobalOptions();

            processOptions(commandInvocation);

            return process(commandInvocation);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
    }

    abstract void initOptions();

    abstract String suggestHelp();


    void processOptions(CommandInvocation commandInvocation) {

        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("URI not specified");
        }

        Iterator<String> it = args.iterator();

        while (it.hasNext()) {
            String option = it.next();
            switch (option) {
                case "-s":
                case "--set": {
                    if (!it.hasNext()) {
                        throw new IllegalArgumentException("Option " + option + " requires a value");
                    }
                    String[] keyVal = parseKeyVal(it.next());
                    attrs.add(new AttributeOperation(SET, keyVal[0], keyVal[1]));
                    break;
                }
                case "-d":
                case "--delete": {
                    attrs.add(new AttributeOperation(DELETE, it.next()));
                    break;
                }
                case "-h":
                case "--header": {
                    requireValue(it, option);
                    String[] keyVal = parseKeyVal(it.next());
                    headers.add(keyVal[0], keyVal[1]);
                    break;
                }
                case "-q":
                case "--query": {
                    if (!it.hasNext()) {
                        throw new IllegalArgumentException("Option " + option + " requires a value");
                    }
                    String arg = it.next();
                    String[] keyVal;
                    if (arg.indexOf("=") == -1) {
                        keyVal = new String[] {"", arg};
                    } else {
                        keyVal = parseKeyVal(arg);
                    }
                    filter.put(keyVal[0], keyVal[1]);
                    break;
                }
                default: {
                    if (url == null) {
                        url = option;
                    } else {
                        throw new IllegalArgumentException("Invalid option: " + option);
                    }
                }
            }
        }


        if (url == null) {
            throw new IllegalArgumentException("Resource URI not specified");
        }

        if (outputResult && returnId) {
            throw new IllegalArgumentException("Options -o and -i are mutually exclusive");
        }

        try {
            outputFormat = OutputFormat.valueOf(format.toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Unsupported output format: " + format);
        }

        if (mergeMode && noMerge) {
            throw new IllegalArgumentException("Options --merge and --no-merge are mutually exclusive");
        }

        if (body != null && file != null) {
            throw new IllegalArgumentException("Options --body and --file are mutually exclusive");
        }

        if (file == null && attrs.size() > 0 && !noMerge) {
            mergeMode = true;
        }
    }



    public CommandResult process(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        // see if Content-Type header is explicitly set to non-json value
        Header ctype = headers.get("content-type");

        InputStream content = null;

        CmdStdinContext<JsonNode> ctx = new CmdStdinContext<>();

        if (file != null) {
            if (ctype != null && !"application/json".equals(ctype.getValue())) {
                if ("-".equals(file)) {
                    content = System.in;
                } else {
                    try {
                        content = new BufferedInputStream(new FileInputStream(file));
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException("File not found: " + file);
                    }
                }
            } else {
                ctx = parseFileOrStdin(file);
            }
        } else if (body != null) {
            content = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);

        setupTruststore(config, commandInvocation);

        String auth = null;

        config = ensureAuthInfo(config, commandInvocation);
        config = copyWithServerInfo(config);
        if (credentialsAvailable(config)) {
            auth = ensureToken(config);
        }

        auth = auth != null ? "Bearer " + auth : null;

        if (auth != null) {
            headers.addIfMissing("Authorization", auth);
        }


        final String server = config.getServerUrl();
        final String realm = getTargetRealm(config);
        final String adminRoot = adminRestRoot != null ? adminRestRoot : composeAdminRoot(server);


        String resourceUrl = composeResourceUrl(adminRoot, realm, url);
        String typeName = extractTypeNameFromUri(resourceUrl);


        if (filter.size() > 0) {
            resourceUrl = HttpUtil.addQueryParamsToUri(resourceUrl, filter);
        }

        headers.addIfMissing("Accept", "application/json");

        if (isUpdate() && mergeMode) {
            ObjectNode result;
            HeadersBodyStatus response;
            try {
                response = HttpUtil.doGet(resourceUrl, new HeadersBody(headers));
                checkSuccess(resourceUrl, response);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                copyStream(response.getBody(), buffer);

                result = MAPPER.readValue(buffer.toByteArray(), ObjectNode.class);

            } catch (IOException e) {
                throw new RuntimeException("HTTP request error: " + e.getMessage(), e);
            }

            CmdStdinContext<JsonNode> ctxremote = new CmdStdinContext<>();
            ctxremote.setResult(result);

            // merge local representation over remote one
            if (ctx.getResult() != null) {
                ReflectionUtil.merge(ctx.getResult(), (ObjectNode) ctxremote.getResult());
            }
            ctx = ctxremote;
        }

        if (attrs.size() > 0) {
            if (content != null) {
                throw new RuntimeException("Can't set attributes on content of type other than application/json");
            }

            ctx = mergeAttributes(ctx, MAPPER.createObjectNode(), attrs);
        }

        if (content == null && ctx.getContent() != null) {
            content = new ByteArrayInputStream(ctx.getContent().getBytes(StandardCharsets.UTF_8));
        }

        ReturnFields returnFields = null;

        if (fields != null) {
            returnFields = new ReturnFields(fields);
        }

        // make sure content type is set
        if (content != null) {
            headers.addIfMissing("Content-Type", "application/json");
        }

        LinkedHashMap<String, String> queryParams = new LinkedHashMap<>();
        if (offset != null) {
            queryParams.put("first", String.valueOf(offset));
        }
        if (limit != null) {
            queryParams.put("max", String.valueOf(limit));
        }
        if (queryParams.size() > 0) {
            resourceUrl = HttpUtil.addQueryParamsToUri(resourceUrl, queryParams);
        }

        HeadersBodyStatus response;
        try {
            response = HttpUtil.doRequest(httpVerb, resourceUrl, new HeadersBody(headers, content));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request error: " + e.getMessage(), e);
        }

        // output response
        if (printHeaders) {
            printOut(response.getStatus());
            for (Header header : response.getHeaders()) {
                printOut(header.getName() + ": " + header.getValue());
            }
        }

        checkSuccess(resourceUrl, response);

        AccessibleBufferOutputStream abos = new AccessibleBufferOutputStream(System.out);
        if (response.getBody() == null) {
            throw new RuntimeException("Internal error - response body should never be null");
        }

        if (printHeaders) {
            printOut("");
        }


        Header location = response.getHeaders().get("Location");
        String id = location != null ? extractLastComponentOfUri(location.getValue()) : null;
        if (id != null) {
            if (returnId) {
                printOut(id);
            } else if (!outputResult) {
                printErr("Created new " + typeName + " with id '" + id + "'");
            }
        }

        if (outputResult) {

            if (isCreateOrUpdate() && (response.getStatusCode() == 204 || id != null)) {
                // get object for id
                headers = new Headers();
                if (auth != null) {
                    headers.add("Authorization", auth);
                }
                try {
                    String fetchUrl = id != null ? (resourceUrl + "/" + id) : resourceUrl;
                    response = doGet(fetchUrl, new HeadersBody(headers));
                } catch (IOException e) {
                    throw new RuntimeException("HTTP request error: " + e.getMessage(), e);
                }
            }

            Header contentType = response.getHeaders().get("content-type");
            boolean canPrettyPrint = contentType != null && contentType.getValue().equals("application/json");
            boolean pretty = !compressed;

            if (canPrettyPrint && (pretty || returnFields != null)) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                copyStream(response.getBody(), buffer);

                try {
                    JsonNode rootNode = MAPPER.readValue(buffer.toByteArray(), JsonNode.class);
                    if (returnFields != null) {
                        rootNode = applyFieldFilter(MAPPER, rootNode, returnFields);
                    }
                    if (outputFormat == OutputFormat.JSON) {
                        // now pretty print it to output
                        MAPPER.writeValue(abos, rootNode);
                    } else {
                        printAsCsv(rootNode, returnFields, unquoted);
                    }
                } catch (Exception ignored) {
                    copyStream(new ByteArrayInputStream(buffer.toByteArray()), abos);
                }
            } else {
                copyStream(response.getBody(), abos);
            }
        }

        int lastByte = abos.getLastByte();
        if (lastByte != -1 && lastByte != 13 && lastByte != 10) {
            printErr("");
        }

        return CommandResult.SUCCESS;
    }

    private boolean isUpdate() {
        return "put".equals(httpVerb);
    }

    private boolean isCreateOrUpdate() {
        return "post".equals(httpVerb) || "put".equals(httpVerb);
    }
}
