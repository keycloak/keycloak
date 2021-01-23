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

import org.keycloak.common.ClientConnection;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.urls.UrlType;

import javax.ws.rs.core.HttpHeaders;
import java.net.URI;
import java.util.Locale;

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

    <T> T getContextObject(Class<T> clazz);

    RealmModel getRealm();

    void setRealm(RealmModel realm);

    ClientModel getClient();

    void setClient(ClientModel client);

    ClientConnection getConnection();

    Locale resolveLocale(UserModel user);
    
    /**
     * Get current AuthenticationSessionModel, can be null out of the AuthenticationSession context.
     * 
     * @return current AuthenticationSessionModel or null
     */
    AuthenticationSessionModel getAuthenticationSession(); 
    
    void setAuthenticationSession(AuthenticationSessionModel authenticationSession);
}
