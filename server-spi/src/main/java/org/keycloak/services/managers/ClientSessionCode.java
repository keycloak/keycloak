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

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;

import javax.crypto.Mac;
import java.security.Key;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientSessionCode {

    private static final byte[] HASH_SEPERATOR = "//".getBytes();

    private final RealmModel realm;
    private final ClientSessionModel clientSession;

    public enum ActionType {
        CLIENT,
        LOGIN,
        USER
    }

    public ClientSessionCode(RealmModel realm, ClientSessionModel clientSession) {
        this.realm = realm;
        this.clientSession = clientSession;
    }

    public static ClientSessionCode parse(String code, KeycloakSession session) {
        try {
            String[] parts = code.split("\\.");
            String id = parts[1];

            ClientSessionModel clientSession = session.sessions().getClientSession(id);
            if (clientSession == null) {
                return null;
            }

            String hash = createHash(clientSession.getRealm(), clientSession);
            if (!hash.equals(parts[0])) {
                return null;
            }

            return new ClientSessionCode(clientSession.getRealm(), clientSession);
        } catch (RuntimeException e) {
            return null;
        }
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
            String[] parts = code.split("\\.");
            String id = parts[1];

            result.clientSession = session.sessions().getClientSession(realm, id);
            if (result.clientSession == null) {
                result.clientSessionNotFound = true;
                return result;
            }

            String hash = createHash(realm, result.clientSession);
            if (!hash.equals(parts[0])) {
                result.illegalHash = true;
                return result;
            }

            result.code = new ClientSessionCode(realm, result.clientSession);
            return result;
        } catch (RuntimeException e) {
            result.illegalHash = true;
            return result;
        }
    }



    public static ClientSessionCode parse(String code, KeycloakSession session, RealmModel realm) {
        try {
            String[] parts = code.split("\\.");
            String id = parts[1];

            ClientSessionModel clientSession = session.sessions().getClientSession(realm, id);
            if (clientSession == null) {
                return null;
            }

            String hash = createHash(realm, clientSession);
            if (!hash.equals(parts[0])) {
                return null;
            }

            return new ClientSessionCode(realm, clientSession);
        } catch (RuntimeException e) {
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
        Set<RoleModel> requestedRoles = new HashSet<RoleModel>();
        for (String roleId : clientSession.getRoles()) {
            RoleModel role = realm.getRoleById(roleId);
            if (role != null) {
                requestedRoles.add(role);
            }
        }
        return requestedRoles;
    }

    public Set<ProtocolMapperModel> getRequestedProtocolMappers() {
        Set<ProtocolMapperModel> requestedProtocolMappers = new HashSet<ProtocolMapperModel>();
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
        return generateCode(realm, clientSession);
    }

    private static String generateCode(RealmModel realm, ClientSessionModel clientSession) {
        String hash = createHash(realm, clientSession);

        StringBuilder sb = new StringBuilder();
        sb.append(hash);
        sb.append(".");
        sb.append(clientSession.getId());

        return sb.toString();
    }

    private static String createHash(RealmModel realm, ClientSessionModel clientSession) {
        try {
            Key codeSecretKey = realm.getCodeSecretKey();
            Mac mac = Mac.getInstance(codeSecretKey.getAlgorithm());
            mac.init(codeSecretKey);
            mac.update(clientSession.getId().getBytes());
            mac.update(HASH_SEPERATOR);
            mac.update(clientSession.getNote(ClientSessionModel.ACTION_KEY).getBytes());
            return Base64Url.encode(mac.doFinal());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
