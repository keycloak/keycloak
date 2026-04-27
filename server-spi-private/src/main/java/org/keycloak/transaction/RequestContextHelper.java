/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.transaction;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;

/**
 * Provides some info about current HTTP request. Useful for example for logging
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RequestContextHelper {

    public static final String SESSION_ATTRIBUTE = "REQ_CONTEXT_HELPER";

    private static final Set<String> ALLOWED_ATTRIBUTES = Set.of(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE, OAuth2Constants.SCOPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE,
                                                                 OAuth2Constants.DEVICE_CODE_GRANT_TYPE, OAuth2Constants.RESPONSE_TYPE);

    private final KeycloakSession session;

    // Explicitly set information about context. This is useful when the request is executed outside of HTTP (for example during periodic cleaner tasks)
    private String contextMessage;

    private RequestContextHelper(KeycloakSession session) {
        this.session = session;
    }

    public void setContextMessage(String message) {
        this.contextMessage = message;
    }

    public static RequestContextHelper getContext(KeycloakSession session) {
        RequestContextHelper ctxHelper = (RequestContextHelper) session.getAttribute(SESSION_ATTRIBUTE);
        if (ctxHelper != null) {
            return ctxHelper;
        } else {
            ctxHelper = new RequestContextHelper(session);
            session.setAttribute(SESSION_ATTRIBUTE, ctxHelper);
            return ctxHelper;
        }
    }

    /**
     * Providing short information about current request. For example just something "HTTP GET /realms/test/account"
     *
     * @return
     */
    public String getContextInfo() {
        if (contextMessage != null) return contextMessage;

        try {
            HttpRequest httpRequest = session.getContext().getHttpRequest();
            if (httpRequest != null && httpRequest.getUri() != null) {

                return new StringBuilder("HTTP ")
                        .append(httpRequest.getHttpMethod())
                        .append(" ")
                        .append(httpRequest.getUri().getPath())
                        .toString();
            }
        } catch (Exception e) {
            return "Unknown context";
        }
        return "Non-HTTP context";
    }

    /**
     * Providing longer information about current request. For example something like "HTTP GET /realms/test/protocol/openid-connect/token, form parameters [ grant_type=code, redirect_uri=https://... ]"
     *
     * @return
     */
    public String getDetailedContextInfo() {
        try {
            HttpRequest httpRequest = session.getContext().getHttpRequest();
            if (httpRequest != null && httpRequest.getUri() != null) {
                StringBuilder builder = new StringBuilder("HTTP ")
                        .append(httpRequest.getHttpMethod())
                        .append(" ")
                        .append(httpRequest.getUri().getRequestUri());

                MultivaluedMap<String, String> formParams = httpRequest.getDecodedFormParameters();
                if (formParams != null && !formParams.isEmpty()) {

                    builder.append(", Form parameters [ ");
                    formParams.entrySet().forEach(entry -> {
                        String key = entry.getKey();
                        List<String> values = entry.getValue();
                        values.forEach(value -> {

                            if (!ALLOWED_ATTRIBUTES.contains(key)) value = "***";
                            builder.append(key + "=" + value + ", ");
                        });
                    });
                    builder.append(" ]");
                }

                return builder.toString();
            }
        } catch (Exception e) {
            // Fallback to getContextInfo if this happens
        }
        return getContextInfo();
    }
}
