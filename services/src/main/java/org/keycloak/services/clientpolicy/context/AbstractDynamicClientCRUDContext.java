/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;

abstract class AbstractDynamicClientCRUDContext implements ClientCRUDContext {

    private final JsonWebToken token;
    private ClientModel authenticatedClient;
    private UserModel authenticatedUser;

    public AbstractDynamicClientCRUDContext(KeycloakSession session, JsonWebToken token, RealmModel realm) {
        this.token = token;
        if (token == null) {
            return;
        }
        if (token.getIssuedFor() != null) {
            this.authenticatedClient = realm.getClientByClientId(token.getIssuedFor());
        }
        if (token.getSubject() != null) {
            this.authenticatedUser = session.users().getUserById(realm, token.getSubject());
        }
    }

    @Override
    public ClientModel getAuthenticatedClient() {
        return authenticatedClient;
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return authenticatedUser;
    }

    @Override
    public JsonWebToken getToken() {
        return token;
    }
}
