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
package org.keycloak.services.managers;

import jakarta.ws.rs.NotAuthorizedException;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.util.TokenUtil;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AppAuthManager extends AuthenticationManager {

    private static final String BEARER = "Bearer";

    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    @Override
    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm) {
        AuthResult authResult = super.authenticateIdentityCookie(session, realm);
        if (authResult == null) return null;
        // refresh the cookies!
        createLoginCookie(session, realm, authResult.getUser(), authResult.getSession(), session.getContext().getUri(), session.getContext().getConnection());
        if (authResult.getSession().isRememberMe()) createRememberMeCookie(authResult.getUser().getUsername(), session.getContext().getUri(), session);
        return authResult;
    }

    /**
     * Extracts the token string from the given Authorization Bearer header.
     *
     * @return the token string or {@literal null}
     */
    private static String extractTokenStringFromAuthHeader(String authHeader) {

        if (authHeader == null) {
            return null;
        }

        String[] split = WHITESPACES.split(authHeader.trim());
        if (split.length != 2){
            return null;
        }

        String typeString = split[0];

        if (!Profile.isFeatureEnabled(Profile.Feature.DPOP)) {
            if (!typeString.equalsIgnoreCase(BEARER)) {
                return null;
            }
        } else {
            // "Bearer" is case-insensitive for historical reasons. "DPoP" is case-sensitive to follow the spec.
            if (!typeString.equalsIgnoreCase(BEARER) && !typeString.equals(TokenUtil.TOKEN_TYPE_DPOP)){
                return null;
            }
        }

        String tokenString = split[1];
        if (ObjectUtil.isBlank(tokenString)) {
            return null;
        }

        return tokenString;
    }

    /**
     * Extracts the token string from the Authorization Bearer Header.
     *
     * @param headers
     * @return the token string or {@literal null} if the Authorization header is not of type Bearer, or the token string is missing.
     */
    public static String extractAuthorizationHeaderTokenOrReturnNull(HttpHeaders headers) {
        // error if including more than one Authorization header
        List<String> authHeaders = headers.getRequestHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        if (authHeaders.size() != 1) {
            throw new NotAuthorizedException(BEARER);
        }
        String authHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return extractTokenStringFromAuthHeader(authHeader);
    }

    /**
     * Extracts the token string from the Authorization Bearer Header.
     *
     * @param headers
     * @return the token string or {@literal null} of the Authorization header is missing
     * @throws  NotAuthorizedException if the Authorization header is not of type Bearer, or the token string is missing.
     */
    public static String extractAuthorizationHeaderToken(HttpHeaders headers) {
        String authHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null) {
            return null;
        }
        String tokenString = extractTokenStringFromAuthHeader(authHeader);
        if (tokenString == null ){
            throw new NotAuthorizedException(BEARER);
        }
        return tokenString;
    }

    public static class BearerTokenAuthenticator {
        private KeycloakSession session;
        private RealmModel realm;
        private UriInfo uriInfo;
        private ClientConnection connection;
        private HttpHeaders headers;
        private String tokenString;
        private String audience;

        public BearerTokenAuthenticator(KeycloakSession session) {
            this.session = session;
        }

        public BearerTokenAuthenticator setSession(KeycloakSession session) {
            this.session = session;
            return this;
        }

        public BearerTokenAuthenticator setRealm(RealmModel realm) {
            this.realm = realm;
            return this;
        }

        public BearerTokenAuthenticator setUriInfo(UriInfo uriInfo) {
            this.uriInfo = uriInfo;
            return this;
        }

        public BearerTokenAuthenticator setConnection(ClientConnection connection) {
            this.connection = connection;
            return this;
        }

        public BearerTokenAuthenticator setHeaders(HttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        public BearerTokenAuthenticator setTokenString(String tokenString) {
            this.tokenString = tokenString;
            return this;
        }

        public BearerTokenAuthenticator setAudience(String audience) {
            this.audience = audience;
            return this;
        }

        public AuthResult authenticate() {
            KeycloakContext ctx = session.getContext();
            if (realm == null) realm = ctx.getRealm();
            if (uriInfo == null) uriInfo = ctx.getUri();
            if (connection == null) connection = ctx.getConnection();
            if (headers == null) headers = ctx.getRequestHeaders();
            if (tokenString == null) tokenString = extractAuthorizationHeaderToken(headers);
            // audience can be null

            return verifyIdentityToken(session, realm, uriInfo, connection, true, true, audience, false, tokenString, headers);
        }
    }

}
