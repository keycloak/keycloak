/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.authzen;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.common.Profile;

/**
 * Echoes the {@code X-Request-ID} request header in the response for AuthZen endpoints,
 * as recommended by the OpenID AuthZen Authorization API 1.0 specification (Section 10.1.3).
 * <p>
 * This filter runs after exception mappers, ensuring the header is present on all responses
 * including 4xx and 5xx error responses.
 */
@Provider
public class AuthZenRequestIdFilter implements ContainerResponseFilter {

    public static final String X_REQUEST_ID = "X-Request-ID";
    private static final String AUTHZEN_PATH_SEGMENT = AuthZenRealmResourceProviderFactory.PROVIDER_ID + "/" + AuthZen.AUTHZEN_ACCESS_PATH;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (!Profile.isFeatureEnabled(Profile.Feature.AUTHZEN)) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if (path == null || !path.contains(AUTHZEN_PATH_SEGMENT)) {
            return;
        }

        String requestId = requestContext.getHeaderString(X_REQUEST_ID);
        if (requestId != null) {
            responseContext.getHeaders().putSingle(X_REQUEST_ID, requestId);
        }
    }
}
