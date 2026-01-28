/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.cors;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.representations.AccessToken;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Cors extends Provider {

    long DEFAULT_MAX_AGE = TimeUnit.HOURS.toSeconds(1);
    String DEFAULT_ALLOW_METHODS = "GET, HEAD, OPTIONS";
    Set<String> DEFAULT_ALLOW_HEADERS = Set.of(
            "Origin",
            "Accept",
            "X-Requested-With",
            "Content-Type",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "DPoP");

    String ORIGIN_HEADER = "Origin";
    String AUTHORIZATION_HEADER = "Authorization";

    String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    String ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD = "*";

    static Cors builder() {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        return session.getProvider(Cors.class);
    }

    Cors builder(ResponseBuilder builder);

    Cors preflight();

    Cors auth();

    Cors failOnInvalidOrigin();

    Cors allowAllOrigins();

    Cors allowedOrigins(KeycloakSession session, ClientModel client);

    Cors allowedOrigins(AccessToken token);

    Cors allowedOrigins(String... allowedOrigins);

    Cors allowedOrigins(List<String> allowedOrigins);

    Cors addAllowedOrigins(List<String> allowedOrigins);

    Cors allowedMethods(String... allowedMethods);

    Cors exposedHeaders(String... exposedHeaders);

    /**
     * Add the CORS headers to the current {@link org.keycloak.http.HttpResponse}.
     */
    void add();

    /**
     * <p>Add the CORS headers to the current server {@link org.keycloak.http.HttpResponse} and returns a {@link Response} based
     * on the given {@code builder}.
     *
     * <p>This is a convenient method to make it easier to return a {@link Response} from methods while at the same time
     * adding the corresponding CORS headers to the underlying server response.
     *
     * @param builder the response builder
     * @return the response built from the response builder
     */
    default Response add(ResponseBuilder builder) {
        if (builder == null) {
            throw new IllegalStateException("builder is not set");
        }

        add();

        return builder.build();
    }
}
