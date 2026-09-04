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

package org.keycloak.tests.oauth.tokenexchange;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;

final class TransientSessionValidationOnServer implements FetchOnServer {

    private final String realmName;
    private final String tokenString;

    TransientSessionValidationOnServer(String realmName, String tokenString) {
        this.realmName = realmName;
        this.tokenString = tokenString;
    }

    @Override
    public Object run(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        ClientModel requester = session.clients().getClientByClientId(realm, "requester-client");
        AccessToken token = session.tokens().decode(tokenString, AccessToken.class);

        UserSessionUtil.UserSessionValidationResult first = UserSessionUtil.findValidSessionForAccessToken(
                session, realm, token, requester, ignored -> {});
        if (first.getError() != null) {
            return false;
        }
        AuthenticatedClientSessionModel clientSession = first.getUserSession().getAuthenticatedClientSessionByClient(requester.getId());
        if (clientSession == null) {
            return false;
        }
        clientSession.detachFromUserSession();

        UserSessionUtil.UserSessionValidationResult second = UserSessionUtil.findValidSessionForAccessToken(
                session, realm, token, requester, ignored -> {});
        UserSessionUtil.UserSessionValidationResult third = UserSessionUtil.findValidSessionForAccessToken(
                session, realm, token, requester, ignored -> {});

        return second.getError() == null && third.getError() == null
                && second.getUserSession().getAuthenticatedClientSessionByClient(requester.getId()) != null
                && third.getUserSession().getAuthenticatedClientSessionByClient(requester.getId()) != null;
    }
}
