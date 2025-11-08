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

package org.keycloak.admin.client.resource;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.client.token.TokenManager;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class BearerAuthFilter implements ClientRequestFilter, ClientResponseFilter {

    public static final String AUTH_HEADER_PREFIX = "Bearer ";
    private final String tokenString;
    protected final TokenManager tokenManager;

    public BearerAuthFilter(String tokenString) {
        this.tokenString = tokenString;
        this.tokenManager = null;
    }

    public BearerAuthFilter(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.tokenString = null;
    }


    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String authHeader = (tokenManager != null ? tokenManager.getAccessTokenString() : tokenString);
        if (!authHeader.startsWith(AUTH_HEADER_PREFIX)) {
            authHeader = AUTH_HEADER_PREFIX + authHeader;
        }
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        if (responseContext.getStatus() == 401 && tokenManager != null) {
            List<Object> authHeaders = requestContext.getHeaders().get(HttpHeaders.AUTHORIZATION);
            if (authHeaders == null) {
                return;
            }
            for (Object authHeader : authHeaders) {
                if (authHeader instanceof String) {
                    String headerValue = (String) authHeader;
                    String authHeaderPrefix = getAuthHeaderPrefix();
                    if (headerValue.startsWith(authHeaderPrefix)) {
                        String token = headerValue.substring( authHeaderPrefix.length() );
                        tokenManager.invalidate( token );
                    }
                }
            }
        }
    }

    protected String getAuthHeaderPrefix() {
        return AUTH_HEADER_PREFIX;
    }
}
