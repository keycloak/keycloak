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

import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AppAuthManager extends AuthenticationManager {

    @Override
    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm) {
        AuthResult authResult = super.authenticateIdentityCookie(session, realm);
        if (authResult == null) return null;
        // refresh the cookies!
        createLoginCookie(session, realm, authResult.getUser(), authResult.getSession(), session.getContext().getUri(), session.getContext().getConnection());
        if (authResult.getSession().isRememberMe()) createRememberMeCookie(realm, authResult.getUser().getUsername(), session.getContext().getUri(), session.getContext().getConnection());
        return authResult;
    }

    public String extractAuthorizationHeaderToken(HttpHeaders headers) {
        String tokenString = null;
        String authHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
            String[] split = authHeader.trim().split("\\s+");
            if (split == null || split.length != 2) throw new UnauthorizedException("Bearer");
            if (!split[0].equalsIgnoreCase("Bearer")) throw new UnauthorizedException("Bearer");
            tokenString = split[1];
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
        String tokenString = extractAuthorizationHeaderToken(headers);
        if (tokenString == null) return null;
        AuthResult authResult = verifyIdentityToken(session, realm, uriInfo, connection, true, true, false, tokenString, headers);
        return authResult;
    }

}
