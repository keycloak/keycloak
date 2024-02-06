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

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.keycloak.common.util.Resteasy;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.representations.AccessToken;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Cors extends Provider {

    public static final long DEFAULT_MAX_AGE = TimeUnit.HOURS.toSeconds(1);
    public static final String DEFAULT_ALLOW_METHODS = "GET, HEAD, OPTIONS";
    public static final String DEFAULT_ALLOW_HEADERS = "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, DPoP";

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

    public static Cors add(HttpRequest request, ResponseBuilder response) {
        KeycloakSession session = Resteasy.getContextData(KeycloakSession.class);
        return session.getProvider(Cors.class).request(request).builder(response);
    }

    public static Cors add(HttpRequest request) {
        KeycloakSession session = Resteasy.getContextData(KeycloakSession.class);
        return session.getProvider(Cors.class).request(request);
    }

    public Cors request(HttpRequest request);

    public Cors builder(ResponseBuilder builder);

    public Cors preflight();

    public Cors auth();

    public Cors allowAllOrigins();

    public Cors allowedOrigins(KeycloakSession session, ClientModel client);

    public Cors allowedOrigins(AccessToken token);

    public Cors allowedOrigins(String... allowedOrigins);

    public Cors allowedMethods(String... allowedMethods);

    public Cors exposedHeaders(String... exposedHeaders);

    public Cors addExposedHeaders(String... exposedHeaders);

    public Response build();

    public boolean build(HttpResponse response);

    public boolean build(BiConsumer<String, String> addHeader);

}
