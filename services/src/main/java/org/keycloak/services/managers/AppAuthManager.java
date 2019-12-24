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

import javax.ws.rs.NotAuthorizedException;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
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
        if (authResult.getSession().isRememberMe()) createRememberMeCookie(realm, authResult.getUser().getUsername(), session.getContext().getUri(), session.getContext().getConnection());
        return authResult;
    }

    /**
     * Extracts the token string from the given Authorization Bearer header.
     *
     * @return the token string or {@literal null}
     */
    private String extractTokenStringFromAuthHeader(String authHeader) {

        if (authHeader == null) {
            return null;
        }

        String[] split = WHITESPACES.split(authHeader.trim());
        if (split.length != 2){
            return null;
        }

        String bearerPart = split[0];
        if (!bearerPart.equalsIgnoreCase(BEARER)){
            return null;
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
    public String extractAuthorizationHeaderTokenOrReturnNull(HttpHeaders headers) {
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
    public String extractAuthorizationHeaderToken(HttpHeaders headers) {
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

    public AuthResult authenticateBearerToken(KeycloakSession session, RealmModel realm) {
        KeycloakContext ctx = session.getContext();
        return authenticateBearerToken(session, realm, ctx.getUri(), ctx.getConnection(), ctx.getRequestHeaders());
    }

    public AuthResult authenticateBearerToken(KeycloakSession session) {
        return authenticateBearerToken(session, session.getContext().getRealm(), session.getContext().getUri(), session.getContext().getConnection(), session.getContext().getRequestHeaders());
    }

    public AuthResult authenticateBearerToken(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        return authenticateBearerToken(extractAuthorizationHeaderToken(headers), session, realm, uriInfo, connection, headers);
    }

    public AuthResult authenticateBearerToken(String tokenString, KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        if (tokenString == null) return null;
        AuthResult authResult = verifyIdentityToken(session, realm, uriInfo, connection, true, true, false, tokenString, headers);
        return authResult;
    }

}
