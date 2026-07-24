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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.keycloak.client.admin.cli.CmdStdinContext;
import org.keycloak.client.admin.cli.ReflectionUtil;
import org.keycloak.client.cli.common.AttributeOperation;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.util.AccessibleBufferOutputStream;
import org.keycloak.client.cli.util.Header;
import org.keycloak.client.cli.util.Headers;
import org.keycloak.client.cli.util.HeadersBody;
import org.keycloak.client.cli.util.HeadersBodyStatus;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.client.cli.util.OutputFormat;
import org.keycloak.client.cli.util.ReturnFields;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.entity.ContentType;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.keycloak.client.cli.common.AttributeOperation.Type.DELETE;
import static org.keycloak.client.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.cli.util.HttpUtil.checkSuccess;
import static org.keycloak.client.cli.util.HttpUtil.composeResourceUrl;
import static org.keycloak.client.cli.util.HttpUtil.doGet;
import static org.keycloak.client.cli.util.IoUtil.copyStream;
import static org.keycloak.client.cli.util.IoUtil.printErr;
import static org.keycloak.client.cli.util.IoUtil.printOut;
import static org.keycloak.client.cli.util.OutputUtil.MAPPER;
import static org.keycloak.client.cli.util.OutputUtil.printAsCsv;
import static org.keycloak.client.cli.util.ParseUtil.parseKeyVal;

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

    @Option(names = {"-h", "--header"}, description = "Set request header NAME to VALUE")
    List<String> rawHeaders = new LinkedList<>();

    // to maintain relative positions of set and delete operations
    static class AttributeOperations {
        @Option(names = {"-s", "--set"}, required = true) String set;
        @Option(names = {"-d", "--delete"}, required = true) String delete;
    }

    @ArgGroup(exclusive = true, multiplicity = "0..*")
    List<AttributeOperations> rawAttributeOperations = new ArrayList<>();

    @Option(names = {"-q", "--query"}, description = "Add to request URI a NAME query parameter with value VALUE, for example --query q=username:admin")
    List<String> rawFilters = new LinkedList<>();

    @Parameters(arity = "0..1")
    String uri;

    List<AttributeOperation> attrs = new LinkedList<>();
    Headers headers = new Headers();
    Map<String, String> filter = new HashMap<>();

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

        for (String header : rawHeaders) {
            String[] keyVal = parseKeyVal(header);
            headers.add(keyVal[0], keyVal[1]);
        }

        for (String arg : rawFilters) {
            String[] keyVal = parseKeyVal(arg);
            filter.put(keyVal[0], keyVal[1]);
        }

        if (uri == null) {
            throw new IllegalArgumentException("Resource URI not specified");
        }

        if (outputResult && returnId) {
            throw new IllegalArgumentException("Options -o and -i are mutually exclusive");
        }

        try {
            outputFormat = OutputFormat.valueOf(format.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported output format: " + format);
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

    @Override
    protected boolean nothingToDo() {
        return super.nothingToDo() && file == null && body == null && uri == null && fields == null
                && rawAttributeOperations.isEmpty() && rawFilters.isEmpty() && rawHeaders.isEmpty();
    }

    @Override
    protected void process() {
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
                ctx = CmdStdinContext.parseFileOrStdin(file);
            }
        } else if (body != null) {
            content = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);

        setupTruststore(config);

        String auth = null;

        config = ensureAuthInfo(config);
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


        String resourceUrl = composeResourceUrl(adminRoot, realm, uri);
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
            try {
                ctx.setContent(JsonSerialization.writeValueAsString(ctxremote.getResult()));
            } catch (IOException e) {
                throw new RuntimeException("Could not convert merge result to string " + e.getMessage(), e);
            }
        }

        if (attrs.size() > 0) {
            if (content != null) {
                throw new RuntimeException("Can't set attributes on content of type other than application/json");
            }

            ctx = CmdStdinContext.mergeAttributes(ctx, MAPPER.createObjectNode(), attrs);
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
            if (isCreateOrUpdate() && (response.getStatusCode() == 204 || id != null) && isGetByID(uri)) {
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

            boolean json = response.getHeaders().getContentType().map(ContentType::getMimeType)
                    .filter("application/json"::equals).isPresent();

            if (json && !compressed) {
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
                } catch (Exception e) {
                    throw new RuntimeException("Error processing results: " + e.getMessage(), e);
                }
            } else {
                if (outputFormat != OutputFormat.JSON || returnFields != null) {
                    printErr("Cannot create CSV nor filter returned fields because the response is " + (compressed ? "compressed":"not json"));
                    return;
                }
                // in theory the user could explicitly request json, but this could be a non-json response
                // since there's no option for raw and we don't differentiate the default, there's no error about this
                copyStream(response.getBody(), abos);
            }
        }

        int lastByte = abos.getLastByte();
        if (lastByte != -1 && lastByte != 13 && lastByte != 10) {
            printErr("");
        }
    }

    private boolean isUpdate() {
        return "put".equals(httpVerb);
    }

    private boolean isCreateOrUpdate() {
        return "post".equals(httpVerb) || "put".equals(httpVerb);
    }

    private boolean isGetByID(String url) {
        return !"clients-initial-access".equals(url);
    }
}
