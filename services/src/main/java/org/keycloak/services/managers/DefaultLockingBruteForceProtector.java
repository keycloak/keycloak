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

import java.util.Set;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class DefaultLockingBruteForceProtector extends DefaultBlockingBruteForceProtector {

    private final KeycloakSession session;

    public DefaultLockingBruteForceProtector(KeycloakSessionFactory factory, KeycloakSession session) {
        super(factory);
        this.session = session;
    }

    @Override
    protected void processLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, boolean success, Set<String> categories) {
        // This is used with the JPA implementation which will lock the database entry to prevent concurrent updates.
        // Not spawning a new thread ensures that once the login returns, the data is written to the database, and no concurrent execution is happening,
        // making the next attempt fail due to the logic in DefaultBlockingBruteForceProtector.
        if (success) {
            success(session, realm, user.getId(), categories);
        } else {
            failure(session, realm, user.getId(), clientConnection.getRemoteHost(), Time.currentTimeMillis(), categories);
        }
    }

}
