/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.authorization.cip;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.util.EntityUtils;
import org.keycloak.adapters.authorization.ClaimInformationPointProvider;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.authorization.util.JsonUtils;
import org.keycloak.adapters.authorization.util.PlaceHolders;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpClaimInformationPointProvider implements ClaimInformationPointProvider {

    private final Map<String, Object> config;
    private final HttpClient httpClient;

    public HttpClaimInformationPointProvider(Map<String, Object> config, PolicyEnforcer policyEnforcer) {
        this.config = config;
        this.httpClient = policyEnforcer.getDeployment().getClient();
    }

    @Override
    public Map<String, List<String>> resolve(HttpFacade httpFacade) {
        try {
            InputStream responseStream = executeRequest(httpFacade);

            try (InputStream inputStream = new BufferedInputStream(responseStream)) {
                JsonNode jsonNode = JsonSerialization.mapper.readTree(inputStream);
                Map<String, List<String>> claims = new HashMap<>();
                Map<String, Object> claimsDef = (Map<String, Object>) config.get("claims");

                if (claimsDef == null) {
                    Iterator<String> nodeNames = jsonNode.fieldNames();

                    while (nodeNames.hasNext()) {
                        String nodeName = nodeNames.next();
                        claims.put(nodeName, JsonUtils.getValues(jsonNode.get(nodeName)));
                    }
                } else {
                    for (Entry<String, Object> claimDef : claimsDef.entrySet()) {
                        List<String> jsonPaths = new ArrayList<>();

                        if (claimDef.getValue() instanceof Collection) {
                            jsonPaths.addAll(Collection.class.cast(claimDef.getValue()));
                        } else {
                            jsonPaths.add(claimDef.getValue().toString());
                        }

                        List<String> claimValues = new ArrayList<>();

                        for (String path : jsonPaths) {
                            claimValues.addAll(JsonUtils.getValues(jsonNode, path));
                        }

                        claims.put(claimDef.getKey(), claimValues);
                    }
                }

                return claims;
            }
        } catch (IOException cause) {
            throw new RuntimeException("Could not obtain claims from http claim information point [" + config.get("url") + "] response", cause);
        }
    }

    private InputStream executeRequest(HttpFacade httpFacade) {
        String method = config.get("method").toString();

        if (method == null) {
            method = "GET";
        }

        RequestBuilder builder = null;

        if ("GET".equalsIgnoreCase(method)) {
            builder = RequestBuilder.get();
        } else {
            builder = RequestBuilder.post();
        }

        builder.setUri(config.get("url").toString());

        byte[] bytes = new byte[0];

        try {
            setParameters(builder, httpFacade);

            if (config.containsKey("headers")) {
                setHeaders(builder, httpFacade);
            }

            HttpResponse response = httpClient.execute(builder.build());
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                bytes = EntityUtils.toByteArray(entity);
            }

            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpResponseException("Unexpected response from server: " + statusCode + " / " + statusLine.getReasonPhrase(), statusCode, statusLine.getReasonPhrase(), bytes);
            }

            return new ByteArrayInputStream(bytes);
        } catch (Exception cause) {
            try {
                throw new RuntimeException("Error executing http method [" + builder + "]. Response : " + StreamUtil.readString(new ByteArrayInputStream(bytes), Charset.forName("UTF-8")), cause);
            } catch (Exception e) {
                throw new RuntimeException("Error executing http method [" + builder + "]", cause);
            }
        }
    }

    private void setHeaders(RequestBuilder builder, HttpFacade httpFacade) {
        Object headersDef = config.get("headers");

        if (headersDef != null) {
            Map<String, Object> headers = Map.class.cast(headersDef);

            for (Entry<String, Object> header : headers.entrySet()) {
                Object value = header.getValue();
                List<String> headerValues = new ArrayList<>();

                if (value instanceof Collection) {
                    Collection values = Collection.class.cast(value);

                    for (Object item : values) {
                        headerValues.addAll(PlaceHolders.resolve(item.toString(), httpFacade));
                    }
                } else {
                    headerValues.addAll(PlaceHolders.resolve(value.toString(), httpFacade));
                }

                for (String headerValue : headerValues) {
                    builder.addHeader(header.getKey(), headerValue);
                }
            }
        }
    }

    private void setParameters(RequestBuilder builder, HttpFacade httpFacade) {
        Object config = this.config.get("parameters");

        if (config != null) {
            Map<String, Object> paramsDef = Map.class.cast(config);

            for (Entry<String, Object> paramDef : paramsDef.entrySet()) {
                Object value = paramDef.getValue();
                List<String> paramValues = new ArrayList<>();

                if (value instanceof Collection) {
                    Collection values = Collection.class.cast(value);

                    for (Object item : values) {
                        paramValues.addAll(PlaceHolders.resolve(item.toString(), httpFacade));
                    }
                } else {
                    paramValues.addAll(PlaceHolders.resolve(value.toString(), httpFacade));
                }

                for (String paramValue : paramValues) {
                    builder.addParameter(paramDef.getKey(), paramValue);
                }
            }
        }
    }
}
