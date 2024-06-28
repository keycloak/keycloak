/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.authorization.spi;

/**
 * Represents an outgoing HTTP response and the contract to manipulate it.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface HttpResponse {

    /**
     * Send an error with the given {@code statusCode}.
     *
     * @param statusCode the status to set in the response
     */
    void sendError(int statusCode);

    /**
     * Send an error with the given {@code statusCode} and {@code reason} message.
     *
     * @param statusCode the status to set in the response
     */
    void sendError(int statusCode, String reason);

    /**
     * Set a header with the given {@code name} and {@code value}.
     *
     * @param name the header name
     * @param value the header value
     */
    void setHeader(String name, String value);
}
