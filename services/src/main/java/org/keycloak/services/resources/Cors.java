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
package org.keycloak.services.resources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.utils.WebOriginsUtils;
import org.keycloak.representations.AccessToken;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Cors {

    private static final Logger logger = Logger.getLogger(Cors.class);

    public static final long DEFAULT_MAX_AGE = TimeUnit.HOURS.toSeconds(1);
    public static final String DEFAULT_ALLOW_METHODS = "GET, HEAD, OPTIONS";
    public static final String DEFAULT_ALLOW_HEADERS = "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers";

    public static final String ORIGIN_HEADER = "Origin";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD = "*";
    public static final String INCLUDE_REDIRECTS = "+";

    private HttpRequest request;
    private ResponseBuilder builder;
    private Set<String> allowedOrigins;
    private Set<String> allowedMethods;
    private Set<String> exposedHeaders;

    private boolean preflight;
    private boolean auth;

    public Cors(HttpRequest request, ResponseBuilder response) {
        this.request = request;
        this.builder = response;
    }

    public Cors(HttpRequest request) {
        this.request = request;
    }

    public static Cors add(HttpRequest request, ResponseBuilder response) {
        return new Cors(request, response);
    }

    public static Cors add(HttpRequest request) {
        return new Cors(request);
    }

    public Cors preflight() {
        preflight = true;
        return this;
    }

    public Cors auth() {
        auth = true;
        return this;
    }

    public Cors allowedOrigins(UriInfo uriInfo, ClientModel client) {
        if (client != null) {
            allowedOrigins = WebOriginsUtils.resolveValidWebOrigins(uriInfo, client);
        }
        return this;
    }

    public Cors allowedOrigins(AccessToken token) {
        if (token != null) {
            allowedOrigins = token.getAllowedOrigins();
        }
        return this;
    }

    public Cors allowedOrigins(String... allowedOrigins) {
        if (allowedOrigins != null && allowedOrigins.length > 0) {
            this.allowedOrigins = new HashSet<>(Arrays.asList(allowedOrigins));
        }
        return this;
    }

    public Cors allowedMethods(String... allowedMethods) {
        this.allowedMethods = new HashSet<>(Arrays.asList(allowedMethods));
        return this;
    }

    public Cors exposedHeaders(String... exposedHeaders) {
        this.exposedHeaders = new HashSet<>(Arrays.asList(exposedHeaders));
        return this;
    }

    public Response build() {
        String origin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN_HEADER);
        if (origin == null) {
            logger.trace("No origin header ignoring");
            return builder.build();
        }

        if (!preflight && (allowedOrigins == null || (!allowedOrigins.contains(origin) && !allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD)))) {
            if (logger.isDebugEnabled()) {
                logger.debugv("Invalid CORS request: origin {0} not in allowed origins {1}", origin, Arrays.toString(allowedOrigins.toArray()));
            }
            return builder.build();
        }

        if (allowedOrigins != null && allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD)) {
            builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD);
        } else {
            builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }

        if (preflight) {
            if (allowedMethods != null) {
                builder.header(ACCESS_CONTROL_ALLOW_METHODS, CollectionUtil.join(allowedMethods));
            } else {
                builder.header(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ALLOW_METHODS);
            }
        }

        if (!preflight && exposedHeaders != null) {
            builder.header(ACCESS_CONTROL_EXPOSE_HEADERS, CollectionUtil.join(exposedHeaders));
        }

        builder.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(auth));

        if (preflight) {
            if (auth) {
                builder.header(ACCESS_CONTROL_ALLOW_HEADERS, String.format("%s, %s", DEFAULT_ALLOW_HEADERS, AUTHORIZATION_HEADER));
            } else {
                builder.header(ACCESS_CONTROL_ALLOW_HEADERS, DEFAULT_ALLOW_HEADERS);
            }
        }

        if (preflight) {
            builder.header(ACCESS_CONTROL_MAX_AGE, DEFAULT_MAX_AGE);
        }

        logger.debug("Added CORS headers to response");

        return builder.build();
    }

    public void build(HttpResponse response) {
        String origin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN_HEADER);
        if (origin == null) {
            logger.trace("No origin header ignoring");
            return;
        }

        if (!preflight && (allowedOrigins == null || (!allowedOrigins.contains(origin) && !allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD)))) {
            if (logger.isDebugEnabled()) {
                logger.debugv("Invalid CORS request: origin {0} not in allowed origins {1}", origin, Arrays.toString(allowedOrigins.toArray()));
            }
            return;
        }

        if (allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD)) {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD);
        } else {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }

        if (preflight) {
            if (allowedMethods != null) {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, CollectionUtil.join(allowedMethods));
            } else {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ALLOW_METHODS);
            }
        }

        if (!preflight && exposedHeaders != null) {
            response.getOutputHeaders().add(ACCESS_CONTROL_EXPOSE_HEADERS, CollectionUtil.join(exposedHeaders));
        }

        response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(auth));

        if (preflight) {
            if (auth) {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, String.format("%s, %s", DEFAULT_ALLOW_HEADERS, AUTHORIZATION_HEADER));
            } else {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, DEFAULT_ALLOW_HEADERS);
            }
        }

        if (preflight) {
            response.getOutputHeaders().add(ACCESS_CONTROL_MAX_AGE, DEFAULT_MAX_AGE);
        }

        logger.debug("Added CORS headers to response");
    }

}
