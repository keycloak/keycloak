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

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

/**
* @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
*/
public class Auth {

    private final boolean cookie;
    private final RealmModel realm;
    private final AccessToken token;
    private final UserModel user;
    private final ClientModel client;
    private final UserSessionModel session;
    private AuthenticatedClientSessionModel clientSession;

    public Auth(RealmModel realm, AccessToken token, UserModel user, ClientModel client, UserSessionModel session, boolean cookie) {
        this.cookie = cookie;
        this.token = token;
        this.realm = realm;

        this.user = user;
        this.client = client;
        this.session = session;
    }

    public boolean isCookieAuthenticated() {
        return cookie;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public UserModel getUser() {
        return user;
    }

    public ClientModel getClient() {
        return client;
    }

    public AccessToken getToken() {
        return token;
    }

    public UserSessionModel getSession() {
        return session;
    }

    public AuthenticatedClientSessionModel getClientSession() {
        return clientSession;
    }

    public void setClientSession(AuthenticatedClientSessionModel clientSession) {
        this.clientSession = clientSession;
    }

    public void require(String role) {
        if (!hasClientRole(client, role)) {
            throw new ForbiddenException();
        }
    }

    public void requireOneOf(String... roles) {
        if (!hasOneOfAppRole(client, roles)) {
            throw new ForbiddenException();
        }
    }

    public boolean hasRealmRole(String role) {
        if (cookie) {
            return user.hasRole(realm.getRole(role));
        } else {
            AccessToken.Access access = token.getRealmAccess();
            return access != null && access.isUserInRole(role);
        }
    }

    public boolean hasOneOfRealmRole(String... roles) {
        for (String r : roles) {
            if (hasRealmRole(r)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasClientRole(ClientModel app, String role) {
        if (cookie) {
            return user.hasRole(app.getRole(role));
        } else {
            AccessToken.Access access = token.getResourceAccess(app.getClientId());
            return access != null && access.isUserInRole(role);
        }
    }

    public boolean hasOneOfAppRole(ClientModel app, String... roles) {
        for (String r : roles) {
            if (hasClientRole(app, r)) {
                return true;
            }
        }
        return false;
    }

}
