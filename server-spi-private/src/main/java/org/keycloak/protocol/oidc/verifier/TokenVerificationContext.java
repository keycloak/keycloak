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
package org.keycloak.protocol.oidc.verifier;

import java.util.Objects;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.util.TokenUtil;

/**
 * Describes a token and the request in which it is being verified.
 *
 * @param tokenString the serialized token, required when verifying the access token hash in a resource proof
 * @param authorizationScheme the scheme used to present this token, or null when it was supplied independently of the Authorization header
 */
public record TokenVerificationContext(KeycloakSession session, RealmModel realm, UriInfo uriInfo, HttpRequest request,
                                       String tokenString, String authorizationScheme) {

    public TokenVerificationContext(KeycloakSession session, String tokenString) {
        this(session, session.getContext().getRealm(), session.getContext().getUri(), session.getContext().getHttpRequest(), tokenString, null);
    }

    public static TokenVerificationContext fromSession(KeycloakSession session) {
        return new TokenVerificationContext(session, null);
    }

    public static TokenVerificationContext forResource(KeycloakSession session, String tokenString, String authorizationScheme) {
        return forResource(session, session.getContext().getRealm(), session.getContext().getUri(), session.getContext().getHttpRequest(), tokenString, authorizationScheme);
    }

    public static TokenVerificationContext forResource(KeycloakSession session, RealmModel realm, UriInfo uriInfo, HttpRequest request,
                                                       String tokenString, String authorizationScheme) {
        return new TokenVerificationContext(session, realm, uriInfo, request, tokenString,
                Objects.requireNonNullElse(authorizationScheme, TokenUtil.TOKEN_TYPE_BEARER));
    }

    /** The token was supplied separately from any credentials in the Authorization header. */
    public static TokenVerificationContext forResourceParameter(KeycloakSession session, String tokenString) {
        return new TokenVerificationContext(session, tokenString);
    }
}
