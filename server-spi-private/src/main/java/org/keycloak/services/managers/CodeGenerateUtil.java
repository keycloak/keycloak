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

import org.keycloak.models.ClientLoginSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.LoginSessionModel;

/**
 * TODO: More object oriented and rather add parsing/generating logic into the session implementations itself
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class CodeGenerateUtil {

    static <CS extends CommonClientSessionModel> CS parseSession(String code, KeycloakSession session, RealmModel realm, Class<CS> expectedClazz) {
        CommonClientSessionModel result = null;
        if (expectedClazz.equals(ClientSessionModel.class)) {
            try {
                String[] parts = code.split("\\.");
                String id = parts[2];
                result = session.sessions().getClientSession(realm, id);
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        } else if (expectedClazz.equals(LoginSessionModel.class)) {
            result = session.loginSessions().getCurrentLoginSession(realm);
        } else if (expectedClazz.equals(ClientLoginSessionModel.class)) {
            try {
                String[] parts = code.split("\\.");
                String userSessionId = parts[1];
                String clientUUID = parts[2];

                UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
                if (userSession == null) {
                    return null;
                }

                result = userSession.getClientLoginSessions().get(clientUUID);
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Not known impl: " + expectedClazz.getName());
        }

        return expectedClazz.cast(result);
    }

    static String generateCode(CommonClientSessionModel clientSession, String actionId) {
        if (clientSession instanceof ClientSessionModel) {
            StringBuilder sb = new StringBuilder();
            sb.append("cls.");
            sb.append(actionId);
            sb.append('.');
            sb.append(clientSession.getId());

            return sb.toString();
        } else if (clientSession instanceof LoginSessionModel) {
            // Should be sufficient. LoginSession itself is in the cookie
            return actionId;
        } else if (clientSession instanceof ClientLoginSessionModel) {
            String userSessionId = ((ClientLoginSessionModel) clientSession).getUserSession().getId();
            String clientUUID = clientSession.getClient().getId();
            StringBuilder sb = new StringBuilder();
            sb.append("uss.");
            sb.append(actionId);
            sb.append('.');
            sb.append(userSessionId);
            sb.append('.');
            sb.append(clientUUID);
            return sb.toString();
        } else {
            throw new IllegalArgumentException("Not known impl: " + clientSession.getClass().getName());
        }
    }


}
