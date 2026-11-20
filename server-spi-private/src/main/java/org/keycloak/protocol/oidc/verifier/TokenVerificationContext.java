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

/**
 * Request data shared by token validation and binding verification. It deliberately contains no verifier:
 * a grant can verify the binding of an already validated token after authorizing its use.
 *
 * @param tokenString the serialized token, required when verifying the access token hash in a resource proof
 * @param authorizationScheme the scheme used to present this token, not the scheme used for client authentication
 */
public record TokenVerificationContext(KeycloakSession session, RealmModel realm, UriInfo uriInfo, HttpRequest request,
                                       String tokenString, String authorizationScheme, VerificationMode mode) {

    public TokenVerificationContext {
        Objects.requireNonNull(mode, "mode");
    }

    public enum VerificationMode {
        /** Resource authentication with an explicit authorization scheme. */
        RESOURCE_HEADER,
        /** A resource access token in a parameter, independently of client authentication. */
        RESOURCE_PARAMETER,
        /** Standard token exchange checks binding after authorizing the client. */
        TOKEN_EXCHANGE,
        /** Run the provider's token restrictions without requiring proof from the token holder. */
        VALIDATION_ONLY;

        public boolean isResourceRequest() {
            return switch (this) {
                case RESOURCE_HEADER, RESOURCE_PARAMETER -> true;
                case TOKEN_EXCHANGE, VALIDATION_ONLY -> false;
            };
        }
    }

    public static TokenVerificationContext forResource(KeycloakSession session, String tokenString, String authorizationScheme) {
        return forResource(session, session.getContext().getRealm(), session.getContext().getUri(), session.getContext().getHttpRequest(), tokenString, authorizationScheme);
    }

    public static TokenVerificationContext forResource(KeycloakSession session, RealmModel realm, UriInfo uriInfo, HttpRequest request,
                                                       String tokenString, String authorizationScheme) {
        return new TokenVerificationContext(session, realm, uriInfo, request, tokenString, authorizationScheme, VerificationMode.RESOURCE_HEADER);
    }

    public static TokenVerificationContext forResourceParameter(KeycloakSession session, String tokenString) {
        return fromSession(session, tokenString, VerificationMode.RESOURCE_PARAMETER);
    }

    public static TokenVerificationContext forTokenExchange(KeycloakSession session, String tokenString) {
        return fromSession(session, tokenString, VerificationMode.TOKEN_EXCHANGE);
    }

    public static TokenVerificationContext forIntrospection(KeycloakSession session) {
        return fromSession(session, null, VerificationMode.VALIDATION_ONLY);
    }

    public static TokenVerificationContext forAuthenticationChannel(KeycloakSession session) {
        return fromSession(session, null, VerificationMode.VALIDATION_ONLY);
    }

    public static TokenVerificationContext forIdentityToken(KeycloakSession session, RealmModel realm, UriInfo uriInfo, String tokenString) {
        return new TokenVerificationContext(session, realm, uriInfo, session.getContext().getHttpRequest(), tokenString, null, VerificationMode.VALIDATION_ONLY);
    }

    private static TokenVerificationContext fromSession(KeycloakSession session, String tokenString, VerificationMode mode) {
        return new TokenVerificationContext(session, session.getContext().getRealm(), session.getContext().getUri(),
                session.getContext().getHttpRequest(), tokenString, null, mode);
    }
}
