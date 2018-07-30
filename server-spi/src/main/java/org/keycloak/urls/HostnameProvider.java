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

public interface HostnameProvider extends Provider {

    /**
     * Return the hostname. Http headers, realm details, etc. can be retrieved from the KeycloakSession. Do NOT use
     * {@link KeycloakContext#getUri()} as it will in turn call the HostnameProvider resulting in an infinite loop!
     *
     * @param originalUriInfo the original UriInfo before hostname is replaced by the HostnameProvider
     * @return the hostname
     */
    String getHostname(UriInfo originalUriInfo);

    int getPort(UriInfo originalUriInfo);

    @Override
    default void close() {
    }

}
