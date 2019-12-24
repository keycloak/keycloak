/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.util;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Tokens {

    public static AccessToken getAccessToken(KeycloakSession keycloakSession) {
        AppAuthManager authManager = new AppAuthManager();
        KeycloakContext context = keycloakSession.getContext();
        AuthResult authResult = authManager.authenticateBearerToken(keycloakSession, context.getRealm(), context.getUri(), context.getConnection(), context.getRequestHeaders());

        if (authResult != null) {
            return authResult.getToken();
        }

        return null;
    }

    public static AccessToken getAccessToken(String accessToken, KeycloakSession keycloakSession) {
        AppAuthManager authManager = new AppAuthManager();
        KeycloakContext context = keycloakSession.getContext();
        AuthResult authResult = authManager.authenticateBearerToken(accessToken, keycloakSession, context.getRealm(), context.getUri(), context.getConnection(), context.getRequestHeaders());

        if (authResult != null) {
            return authResult.getToken();
        }

        return null;
    }

}
