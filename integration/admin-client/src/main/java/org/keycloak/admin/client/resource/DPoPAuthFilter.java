/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.util.DPoPGenerator;

import static org.keycloak.OAuth2Constants.DPOP_HTTP_HEADER;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DPoPAuthFilter extends BearerAuthFilter {

    private final boolean tokenRequest;

    public DPoPAuthFilter(TokenManager tokenManager, boolean tokenRequest) {
        super(tokenManager);
        this.tokenRequest = tokenRequest;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String requestUri = requestContext.getUri().toString();
        if (tokenRequest) {
            if (requestUri.endsWith("/token")) {
                // Request for obtain new accessToken or refresh-token request
                String dpop = DPoPGenerator.generateRsaSignedDPoPProof(tokenManager.getDpopKeyPair(), requestContext.getMethod(), requestUri, null);
                requestContext.getHeaders().add(DPOP_HTTP_HEADER, dpop);
            }
        } else {
            // Regular request to admin REST API
            String accessToken = tokenManager.getAccessTokenString();
            String dpop = DPoPGenerator.generateRsaSignedDPoPProof(tokenManager.getDpopKeyPair(), requestContext.getMethod(), requestUri, accessToken);
            requestContext.getHeaders().add(DPOP_HTTP_HEADER, dpop);

            String authHeader = DPOP_HTTP_HEADER + " " + accessToken;
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
        }
    }


    @Override
    protected String getAuthHeaderPrefix() {
        return DPOP_HTTP_HEADER;
    }
}
