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

import java.io.InputStream;
import java.util.List;

import org.keycloak.adapters.authorization.TokenPrincipal;

/**
 * Represents an incoming HTTP request and the contract to manipulate it.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface HttpRequest {

    /**
     * Get the request path. This is the path relative to the context path.
     * E.g.: for a HTTP GET request to http://my.appserver.com/my-application/path/sub-path this method is going to return /path/sub-path.

     * @return the relative path
     */
    String getRelativePath();

    /**
     * Returns the name of the HTTP method with which this request was made, for example, GET, POST, or PUT.
     *
     * @return a {@code String} specifying the name of the method with which this request was made
     */
    String getMethod();

    /**
     * Get the URI representation for the current request.
     *
     * @return a {@code String} representation for the current request
     */
    String getURI();

    /**
     * Get a list of all of the values set for the specified header within the HTTP request.
     *
     * @param name the header name
     * @return a list of the values set for this header, if the header is not set on the request then null should be returned
     */
    List<String> getHeaders(String name);

    /**
     * Get the first value for a parameter with the given {@code name}
     *
     * @param name the parameter name
     * @return the value of the parameter
     */
    String getFirstParam(String name);

    /**
     * Get the first value for a cookie with the given {@code name}.
     *
     * @param name the parameter name
     * @return the value of the cookie
     */
    String getCookieValue(String name);

    /**
     * Returns the client address.
     *
     * @return the client address.
     */
    String getRemoteAddr();

    /**
     * Indicates if the request is coming from a secure channel through HTTPS.
     *
     * @return {@code true} if the HTTP scheme is set to 'https'. Otherwise, {@code false}
     */
    boolean isSecure();

    /**
     * Get the first value for a HEADER with the given {@code name}.
     *
     * @param name the HEADER name
     * @return the value of the HEADER
     */
    String getHeader(String name);

    /**
     * Returns the request input stream
     *
     * @param buffered if the input stream should be buffered and support for multiple reads
     * @return the request input stream
     */
    InputStream getInputStream(boolean buffered);

    /**
     * Returns a {@link TokenPrincipal} associated with the request.
     *
     * @return the principal
     */
    TokenPrincipal getPrincipal();
}
