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

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.OAuth2Constants;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientSessionCode {

    private static final String ACTIVE_CODE = "active_code";

    private static final Logger logger = Logger.getLogger(ClientSessionCode.class);

    private static final String NEXT_CODE = ClientSessionCode.class.getName() + ".nextCode";

    private KeycloakSession session;
    private final RealmModel realm;
    private final ClientSessionModel clientSession;

    public enum ActionType {
        CLIENT,
        LOGIN,
        USER
    }

    public ClientSessionCode(KeycloakSession session, RealmModel realm, ClientSessionModel clientSession) {
        this.session = session;
        this.realm = realm;
        this.clientSession = clientSession;
    }

    public static class ParseResult {
        ClientSessionCode code;
        boolean clientSessionNotFound;
        boolean illegalHash;
        ClientSessionModel clientSession;

        public ClientSessionCode getCode() {
            return code;
        }

        public boolean isClientSessionNotFound() {
            return clientSessionNotFound;
        }

        public boolean isIllegalHash() {
            return illegalHash;
        }

        public ClientSessionModel getClientSession() {
            return clientSession;
        }
    }

    public static ParseResult parseResult(String code, KeycloakSession session, RealmModel realm) {
        ParseResult result = new ParseResult();
        if (code == null) {
            result.illegalHash = true;
            return result;
        }
        try {
            result.clientSession = getClientSession(code, session, realm);
            if (result.clientSession == null) {
                result.clientSessionNotFound = true;
                return result;
            }

            if (!verifyCode(code, result.clientSession)) {
                result.illegalHash = true;
                return result;
            }

            result.code = new ClientSessionCode(session, realm, result.clientSession);
            return result;
        } catch (RuntimeException e) {
            result.illegalHash = true;
            return result;
        }
    }

    public static ClientSessionCode parse(String code, KeycloakSession session, RealmModel realm) {
        try {
            ClientSessionModel clientSession = getClientSession(code, session, realm);
            if (clientSession == null) {
                return null;
            }

            if (!verifyCode(code, clientSession)) {
                return null;
            }

            return new ClientSessionCode(session, realm, clientSession);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static ClientSessionModel getClientSession(String code, KeycloakSession session, RealmModel realm) {
        try {
            String[] parts = code.split("\\.");
            String id = parts[1];
            return session.sessions().getClientSession(realm, id);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public ClientSessionModel getClientSession() {
        return clientSession;
    }

    public boolean isValid(String requestedAction, ActionType actionType) {
        if (!isValidAction(requestedAction)) return false;
        return isActionActive(actionType);
    }

    public boolean isActionActive(ActionType actionType) {
        int timestamp = clientSession.getTimestamp();

        int lifespan;
        switch (actionType) {
            case CLIENT:
                lifespan = realm.getAccessCodeLifespan();
                break;
            case LOGIN:
                lifespan = realm.getAccessCodeLifespanLogin() > 0 ? realm.getAccessCodeLifespanLogin() : realm.getAccessCodeLifespanUserAction();
                break;
            case USER:
                lifespan = realm.getAccessCodeLifespanUserAction();
                break;
            default:
                throw new IllegalArgumentException();
        }

        return timestamp + lifespan > Time.currentTime();
    }

    public boolean isValidAction(String requestedAction) {
        String action = clientSession.getAction();
        if (action == null) {
            return false;
        }
        if (!action.equals(requestedAction)) {
            return false;
        }
        return true;
    }


    public Set<RoleModel> getRequestedRoles() {
        Set<RoleModel> requestedRoles = new HashSet<>();
        for (String roleId : clientSession.getRoles()) {
            RoleModel role = realm.getRoleById(roleId);
            if (role != null) {
                requestedRoles.add(role);
            }
        }
        return requestedRoles;
    }

    public Set<ProtocolMapperModel> getRequestedProtocolMappers() {
        Set<ProtocolMapperModel> requestedProtocolMappers = new HashSet<>();
        Set<String> protocolMappers = clientSession.getProtocolMappers();
        ClientModel client = clientSession.getClient();
        ClientTemplateModel template = client.getClientTemplate();
        if (protocolMappers != null) {
            for (String protocolMapperId : protocolMappers) {
                ProtocolMapperModel protocolMapper = client.getProtocolMapperById(protocolMapperId);
                if (protocolMapper == null && template != null) {
                    protocolMapper = template.getProtocolMapperById(protocolMapperId);
                }
                if (protocolMapper != null) {
                    requestedProtocolMappers.add(protocolMapper);
                }
            }
        }
        return requestedProtocolMappers;
    }

    public void setAction(String action) {
        clientSession.setAction(action);
        clientSession.setTimestamp(Time.currentTime());
    }

    public String getCode() {
        String nextCode = (String) session.getAttribute(NEXT_CODE + "." + clientSession.getId());
        if (nextCode == null) {
            nextCode = generateCode(clientSession);
            session.setAttribute(NEXT_CODE + "." + clientSession.getId(), nextCode);
        } else {
            logger.debug("Code already generated for session, using code from session attributes");
        }
        return nextCode;
    }

    private static String generateCode(ClientSessionModel clientSession) {
        try {
            String actionId = KeycloakModelUtils.generateSecret();

            StringBuilder sb = new StringBuilder();
            sb.append(actionId);
            sb.append('.');
            sb.append(clientSession.getId());

            // https://tools.ietf.org/html/rfc7636#section-4
            String codeChallenge = clientSession.getNote(OAuth2Constants.CODE_CHALLENGE);
            String codeChallengeMethod = clientSession.getNote(OAuth2Constants.CODE_CHALLENGE_METHOD);
            if (codeChallenge != null) {
                logger.debugf("PKCE received codeChallenge = %s", codeChallenge);
                if (codeChallengeMethod == null) {
                    logger.debug("PKCE not received codeChallengeMethod, treating plain");
                    codeChallengeMethod = OAuth2Constants.PKCE_METHOD_PLAIN;
                } else {
                    logger.debugf("PKCE received codeChallengeMethod = %s", codeChallengeMethod);
                }
            }

            String code = sb.toString();

            clientSession.setNote(ACTIVE_CODE, code);

            return code;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean verifyCode(String code, ClientSessionModel clientSession) {
        try {
            String activeCode = clientSession.getNote(ACTIVE_CODE);
            if (activeCode == null) {
                logger.debug("Active code not found in client session");
                return false;
            }

            clientSession.removeNote(ACTIVE_CODE);

            return MessageDigest.isEqual(code.getBytes(), activeCode.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
