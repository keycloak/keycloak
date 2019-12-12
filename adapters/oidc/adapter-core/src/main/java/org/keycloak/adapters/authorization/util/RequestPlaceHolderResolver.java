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
package org.keycloak.adapters.authorization.util;

import static org.keycloak.adapters.authorization.util.PlaceHolders.getParameter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Cookie;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RequestPlaceHolderResolver implements PlaceHolderResolver {

    static String NAME = "request";

    @Override
    public List<String> resolve(String placeHolder, HttpFacade httpFacade) {
        String source = placeHolder.substring(placeHolder.indexOf('.') + 1);
        Request request = httpFacade.getRequest();

        if (source.startsWith("parameter")) {
            String parameterName = getParameter(source, "Could not obtain parameter name from placeholder [" + source + "]");
            String parameterValue = request.getQueryParamValue(parameterName);

            if (parameterValue == null) {
                parameterValue = request.getFirstParam(parameterName);
            }

            if (parameterValue != null) {
                return Arrays.asList(parameterValue);
            }
        } else if (source.startsWith("header")) {
            String headerName = getParameter(source, "Could not obtain header name from placeholder [" + source + "]");
            List<String> headerValue = request.getHeaders(headerName);

            if (headerValue != null) {
                return headerValue;
            }
        } else if (source.startsWith("cookie")) {
            String cookieName = getParameter(source, "Could not obtain cookie name from placeholder [" + source + "]");
            Cookie cookieValue = request.getCookie(cookieName);

            if (cookieValue != null) {
                return Arrays.asList(cookieValue.getValue());
            }
        } else if (source.startsWith("remoteAddr")) {
            String value = request.getRemoteAddr();

            if (value != null) {
                return Arrays.asList(value);
            }
        } else if (source.startsWith("method")) {
            String value = request.getMethod();

            if (value != null) {
                return Arrays.asList(value);
            }
        } else if (source.startsWith("uri")) {
            String value = request.getURI();

            if (value != null) {
                return Arrays.asList(value);
            }
        } else if (source.startsWith("relativePath")) {
            String value = request.getRelativePath();

            if (value != null) {
                return Arrays.asList(value);
            }
        } else if (source.startsWith("secure")) {
            return Arrays.asList(String.valueOf(request.isSecure()));
        } else if (source.startsWith("body")) {
            String contentType = request.getHeader("Content-Type");

            if (contentType == null) {
                contentType = "";
            } else if (contentType.indexOf(';') != -1){
                contentType = contentType.substring(0, contentType.indexOf(';')).trim();
            }

            InputStream body = request.getInputStream(true);

            try {
                if (body == null || body.available() == 0) {
                    return Collections.emptyList();
                }
            } catch (IOException cause) {
                throw new RuntimeException("Failed to check available bytes in request input stream", cause);
            }

            if (body.markSupported()) {
                body.mark(0);
            }

            List<String> values = new ArrayList<>();

            try {
                switch (contentType) {
                    case "application/json":
                        try {
                            JsonNode jsonNode = JsonSerialization.mapper.readTree(new BufferedInputStream(body) {
                                @Override
                                public void close() {
                                    // we can't close the stream because it may be used later by the application
                                }
                            });
                            String path = getParameter(source, null);

                            if (path == null) {
                                values.addAll(JsonUtils.getValues(jsonNode));
                            } else {
                                values.addAll(JsonUtils.getValues(jsonNode, path));
                            }
                        } catch (IOException cause) {
                            throw new RuntimeException("Could not extract claim from request JSON body", cause);
                        }
                        break;
                    default:
                        StringBuilder value = new StringBuilder();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(body));

                        try {
                            int ch;

                            while ((ch = reader.read()) != -1) {
                                value.append((char) ch);
                            }
                        } catch (IOException cause) {
                            throw new RuntimeException("Could not extract claim from request body", cause);
                        }

                        values.add(value.toString());
                }
            } finally {
                if (body.markSupported()) {
                    try {
                        body.reset();
                    } catch (IOException cause) {
                        throw new RuntimeException("Failed to reset request input stream", cause);
                    }
                }
            }

            return values;
        }

        return Collections.emptyList();
    }
}
