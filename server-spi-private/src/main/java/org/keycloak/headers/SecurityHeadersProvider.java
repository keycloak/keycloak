/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.headers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.provider.Provider;

public interface SecurityHeadersProvider extends Provider {

    SecurityHeadersOptions options();

    void addHeaders(ContainerRequestContext requestContext, ContainerResponseContext responseContext);

    /**
     * Apply browser security headers to a response header map while a session is available.
     * Respects {@link #options()} (for example {@code allowFrameSrc} for front-channel logout).
     * Used when an async response is resumed after the session is closed and the security-headers
     * filter cannot run.
     */
    void addHeaders(MultivaluedMap<String, Object> responseHeaders);

    @Override
    default void close() {
    }

}
