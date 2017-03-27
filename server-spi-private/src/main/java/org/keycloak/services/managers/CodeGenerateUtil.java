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

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class CodeGenerateUtil {

    private static final Map<Class<? extends CommonClientSessionModel>, ClientSessionParser<?>> PARSERS = new HashMap<>();

    static {
        PARSERS.put(ClientSessionModel.class, new ClientSessionModelParser());
        PARSERS.put(AuthenticationSessionModel.class, new AuthenticationSessionModelParser());
        PARSERS.put(AuthenticatedClientSessionModel.class, new AuthenticatedClientSessionModelParser());
    }


    public static <CS extends CommonClientSessionModel> CS parseSession(String code, KeycloakSession session, RealmModel realm, Class<CS> expectedClazz) {
        ClientSessionParser<?> parser = PARSERS.get(expectedClazz);

        CommonClientSessionModel result = parser.parseSession(code, session, realm);
        return expectedClazz.cast(result);
    }

    public static String generateCode(CommonClientSessionModel clientSession, String actionId) {
        ClientSessionParser parser = getParser(clientSession);
        return parser.generateCode(clientSession, actionId);
    }

    public static void removeExpiredSession(KeycloakSession session, CommonClientSessionModel clientSession) {
        ClientSessionParser parser = getParser(clientSession);
        parser.removeExpiredSession(session, clientSession);
    }

    private static ClientSessionParser<?> getParser(CommonClientSessionModel clientSession) {
        for (Class<?> c : PARSERS.keySet()) {
            if (c.isAssignableFrom(clientSession.getClass())) {
                return PARSERS.get(c);
            }
        }
        return null;
    }


    private interface ClientSessionParser<CS extends CommonClientSessionModel> {

        CS parseSession(String code, KeycloakSession session, RealmModel realm);

        String generateCode(CS clientSession, String actionId);

        void removeExpiredSession(KeycloakSession session, CS clientSession);

    }


    // IMPLEMENTATIONS


    // TODO: remove
    private static class ClientSessionModelParser implements ClientSessionParser<ClientSessionModel> {


        @Override
        public ClientSessionModel parseSession(String code, KeycloakSession session, RealmModel realm) {
            try {
                String[] parts = code.split("\\.");
                String id = parts[2];
                return session.sessions().getClientSession(realm, id);
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }

        @Override
        public String generateCode(ClientSessionModel clientSession, String actionId) {
            StringBuilder sb = new StringBuilder();
            sb.append("cls.");
            sb.append(actionId);
            sb.append('.');
            sb.append(clientSession.getId());

            return sb.toString();
        }

        @Override
        public void removeExpiredSession(KeycloakSession session, ClientSessionModel clientSession) {
            session.sessions().removeClientSession(clientSession.getRealm(), clientSession);
        }
    }


    private static class AuthenticationSessionModelParser implements ClientSessionParser<AuthenticationSessionModel> {

        @Override
        public AuthenticationSessionModel parseSession(String code, KeycloakSession session, RealmModel realm) {
            // Read authSessionID from cookie. Code is ignored for now
            return session.authenticationSessions().getCurrentAuthenticationSession(realm);
        }

        @Override
        public String generateCode(AuthenticationSessionModel clientSession, String actionId) {
            return actionId;
        }

        @Override
        public void removeExpiredSession(KeycloakSession session, AuthenticationSessionModel clientSession) {
            session.authenticationSessions().removeAuthenticationSession(clientSession.getRealm(), clientSession);
        }
    }


    private static class AuthenticatedClientSessionModelParser implements ClientSessionParser<AuthenticatedClientSessionModel> {

        @Override
        public AuthenticatedClientSessionModel parseSession(String code, KeycloakSession session, RealmModel realm) {
            try {
                String[] parts = code.split("\\.");
                String userSessionId = parts[2];
                String clientUUID = parts[3];

                UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
                if (userSession == null) {
                    return null;
                }

                return userSession.getAuthenticatedClientSessions().get(clientUUID);
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }

        @Override
        public String generateCode(AuthenticatedClientSessionModel clientSession, String actionId) {
            String userSessionId = clientSession.getUserSession().getId();
            String clientUUID = clientSession.getClient().getId();
            StringBuilder sb = new StringBuilder();
            sb.append("uss.");
            sb.append(actionId);
            sb.append('.');
            sb.append(userSessionId);
            sb.append('.');
            sb.append(clientUUID);
            return sb.toString();
        }

        @Override
        public void removeExpiredSession(KeycloakSession session, AuthenticatedClientSessionModel clientSession) {
            throw new IllegalStateException("Not yet implemented");
        }

    }


}
