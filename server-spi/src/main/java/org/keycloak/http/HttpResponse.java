/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.http;

import jakarta.ws.rs.core.NewCookie;

/**
 * <p>Represents an out coming HTTP response.
 *
 * <p>Instances of this class can be obtained from {@link org.keycloak.models.KeycloakContext#getHttpResponse}.
 */
public interface HttpResponse {
    /**
     * Gets a status code.
     */
    int getStatus();

    /**
     * Sets a status code.
     *
     * @param statusCode the status code
     */
    void setStatus(int statusCode);

    /**
     * Add a value to the current list of values for the header with the given {@code name}.
     *
     * @param name the header name
     * @param value the header value
     */
    void addHeader(String name, String value);

    /**
     * Set a header. Any existing values will be replaced.
     *
     * @param name the header name
     * @param value the header value
     */
    void setHeader(String name, String value);

    /**
     * Sets a new cookie only if not yet set.
     *
     * @param cookie the cookie
     */
    void setCookieIfAbsent(NewCookie cookie);

}
