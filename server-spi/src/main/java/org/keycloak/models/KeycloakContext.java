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

package org.keycloak.models;

import java.net.URI;
import java.util.Locale;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.Token;
import org.keycloak.common.ClientConnection;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface KeycloakContext {

    URI getAuthServerUrl();

    String getContextPath();

    /**
     * Returns the URI assuming it is a frontend request. To resolve URI for a backend request use {@link #getUri(UrlType)}
     * @return
     */
    KeycloakUriInfo getUri();

    /**
     * Returns the URI. If a frontend request (from user-agent) @frontendRequest should be set to true. If a backend
     * request (request from a client) should be set to false. Depending on the configure hostname provider it may
     * return a hard-coded base URL for frontend request (for example https://auth.mycompany.com) and use the
     * request URL for backend requests. Frontend URI should also be used for realm issuer fields in tokens.
     *
     * @param type the type of the request
     * @return
     */
    KeycloakUriInfo getUri(UrlType type);

    HttpHeaders getRequestHeaders();

    /**
     * Will always return null. You should not need access to a general context object.
     *
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    default <T> T getContextObject(Class<T> clazz) {
        return null;
    }

    RealmModel getRealm();

    void setRealm(RealmModel realm);

    ClientModel getClient();

    void setClient(ClientModel client);

    OrganizationModel getOrganization();

    void setOrganization(OrganizationModel organization);

    ClientConnection getConnection();

    Locale resolveLocale(UserModel user);

    default Locale resolveLocale(UserModel user, Theme.Type themeType) {
        return resolveLocale(user);
    }

    default Locale resolveLocale(UserModel user, boolean ignoreAcceptLanguageHeader) {
        return resolveLocale(user);
    }

    /**
     * Get current AuthenticationSessionModel, can be null out of the AuthenticationSession context.
     *
     * @return current AuthenticationSessionModel or null
     */
    AuthenticationSessionModel getAuthenticationSession();

    void setAuthenticationSession(AuthenticationSessionModel authenticationSession);

    HttpRequest getHttpRequest();

    HttpResponse getHttpResponse();

    void setConnection(ClientConnection clientConnection);

    void setHttpRequest(HttpRequest httpRequest);

    void setHttpResponse(HttpResponse httpResponse);

    UserSessionModel getUserSession();

    void setUserSession(UserSessionModel session);

    /**
     * Returns a {@link Token} representing the bearer token used to authenticate and authorize the current request.
     *
     * @return the bearer token
     */
    Token getBearerToken();

    void setBearerToken(Token token);

    /**
     * Returns the {@link UserModel} bound to this context. The user is first resolved from the {@link #getBearerToken()} set to this
     * context, if any. Otherwise, it will be resolved from the {@link #getUserSession()} set to this context, if any.
     *
     * @return the {@link UserModel} bound to this context.
     */
    UserModel getUser();
}
