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
package org.keycloak.urls;

import org.keycloak.models.KeycloakContext;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.UriInfo;

/**
 * The Hostname provider is used by Keycloak to decide URLs for frontend and backend requests. A provider can either
 * base the URL on the request (Host header for example) or based on hard-coded URLs. Further, it is possible to have
 * different URLs on frontend requests and backend requests.
 *
 * Note: Do NOT use {@link KeycloakContext#getUri()} within a Hostname provider. It will result in an infinite loop.
 */
public interface HostnameProvider extends Provider {

    /**
     * Returns the URL scheme. If not implemented will delegate to {@link #getScheme(UriInfo)}.
     *
     * @param originalUriInfo the original URI
     * @param type type of the request
     * @return the schema
     */
    default String getScheme(UriInfo originalUriInfo, UrlType type) {
        return getScheme(originalUriInfo);
    }

    /**
     * Returns the URL scheme. If not implemented will get the scheme from the request.
     *
     * @param originalUriInfo the original URI
     * @return the schema
     */
    default String getScheme(UriInfo originalUriInfo) {
        return originalUriInfo.getBaseUri().getScheme();
    }

    /**
     * Returns the host. If not implemented will delegate to {@link #getHostname(UriInfo)}.
     *
     * @param originalUriInfo the original URI
     * @param type type of the request
     * @return the host
     */
    default String getHostname(UriInfo originalUriInfo, UrlType type) {
        return getHostname(originalUriInfo);
    }

    /**
     *  Returns the host. If not implemented will get the host from the request.
     * @param originalUriInfo
     * @return the host
     */
    default String getHostname(UriInfo originalUriInfo) {
        return originalUriInfo.getBaseUri().getHost();
    }

    /**
     * Returns the port (or -1 for default port). If not implemented will delegate to {@link #getPort(UriInfo)}
     *
     * @param originalUriInfo the original URI
     * @param type type of the request
     * @return the port
     */
    default int getPort(UriInfo originalUriInfo, UrlType type) {
        return getPort(originalUriInfo);
    }

    /**
     * Returns the port (or -1 for default port). If not implemented will get the port from the request.
     *
     * @param originalUriInfo the original URI
     * @return the port
     */
    default int getPort(UriInfo originalUriInfo) {
        return originalUriInfo.getBaseUri().getPort();
    }

    /**
     * Returns the context-path for Keycloak. This is useful when Keycloak is exposed on a different context-path on
     * a reverse proxy. If not implemented will delegate to {@link #getContextPath(UriInfo)}
     *
     * @param originalUriInfo the original URI
     * @param type type of the request
     * @return the context-path
     */
    default String getContextPath(UriInfo originalUriInfo, UrlType type) {
        return getContextPath(originalUriInfo);
    }

    /**
     * Returns the context-path for Keycloak This is useful when Keycloak is exposed on a different context-path on
     * a reverse proxy. If not implemented will use the context-path from the request, which by default is /auth
     *
     * @param originalUriInfo the original URI
     * @return the context-path
     */
    default String getContextPath(UriInfo originalUriInfo) {
        return originalUriInfo.getBaseUri().getPath();
    }

    @Override
    default void close() {
    }

}
