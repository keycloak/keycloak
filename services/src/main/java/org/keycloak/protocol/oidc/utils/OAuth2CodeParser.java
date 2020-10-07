/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.utils;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.UserSessionCrossDCManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuth2CodeParser {

    private static final Logger logger = Logger.getLogger(OAuth2CodeParser.class);

    private static final Pattern DOT = Pattern.compile("\\.");


    /**
     * Will persist the code to the cache and return the object with the codeData and code correctly set
     *
     * @param session
     * @param clientSession
     * @param codeData
     * @return code parameter to be used in OAuth2 handshake
     */
    public static String persistCode(KeycloakSession session, AuthenticatedClientSessionModel clientSession, OAuth2Code codeData) {
        CodeToTokenStoreProvider codeStore = session.getProvider(CodeToTokenStoreProvider.class);

        UUID key = codeData.getId();
        if (key == null) {
            throw new IllegalStateException("ID not present in the data");
        }

        Map<String, String> serialized = codeData.serializeCode();
        codeStore.put(key, clientSession.getUserSession().getRealm().getAccessCodeLifespan(), serialized);
        return key.toString() + "." + clientSession.getUserSession().getId() + "." + clientSession.getClient().getId();
    }


    /**
     * Will parse the code and retrieve the corresponding OAuth2Code and AuthenticatedClientSessionModel. Will also check if code wasn't already
     * used and if it wasn't expired. If it was already used (or other error happened during parsing), then returned parser will have "isIllegalHash"
     * set to true. If it was expired, the parser will have "isExpired" set to true
     *
     * @param session
     * @param code
     * @param realm
     * @param event
     * @return
     */
    public static ParseResult parseCode(KeycloakSession session, String code, RealmModel realm, EventBuilder event) {
        ParseResult result = new ParseResult(code);

        String[] parsed = DOT.split(code, 3);
        if (parsed.length < 3) {
            logger.warn("Invalid format of the code");
            return result.illegalCode();
        }

        String userSessionId = parsed[1];
        String clientUUID = parsed[2];

        event.detail(Details.CODE_ID, userSessionId);
        event.session(userSessionId);

        // Parse UUID
        UUID codeUUID;
        try {
            codeUUID = UUID.fromString(parsed[0]);
        } catch (IllegalArgumentException re) {
            logger.warn("Invalid format of the UUID in the code");
            return result.illegalCode();
        }

        // Retrieve UserSession
        UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, userSessionId, clientUUID);
        if (userSession == null) {
            // Needed to track if code is invalid or was already used.
            userSession = session.sessions().getUserSession(realm, userSessionId);
            if (userSession == null) {
                return result.illegalCode();
            }
        }

        result.clientSession = userSession.getAuthenticatedClientSessionByClient(clientUUID);

        CodeToTokenStoreProvider codeStore = session.getProvider(CodeToTokenStoreProvider.class);
        Map<String, String> codeData = codeStore.remove(codeUUID);

        // Either code not available or was already used
        if (codeData == null) {
            logger.warnf("Code '%s' already used for userSession '%s' and client '%s'.", codeUUID, userSessionId, clientUUID);
            return result.illegalCode();
        }

        logger.tracef("Successfully verified code '%s'. User session: '%s', client: '%s'", codeUUID, userSessionId, clientUUID);

        result.codeData = OAuth2Code.deserializeCode(codeData);

        // Finally doublecheck if code is not expired
        int currentTime = Time.currentTime();
        if (currentTime > result.codeData.getExpiration()) {
            return result.expiredCode();
        }

        return result;
    }


    public static class ParseResult {

        private final String code;
        private OAuth2Code codeData;
        private AuthenticatedClientSessionModel clientSession;

        private boolean isIllegalCode = false;
        private boolean isExpiredCode = false;


        private ParseResult(String code, OAuth2Code codeData, AuthenticatedClientSessionModel clientSession) {
            this.code = code;
            this.codeData = codeData;
            this.clientSession = clientSession;

            this.isIllegalCode = false;
            this.isExpiredCode = false;
        }


        private ParseResult(String code) {
            this.code = code;
        }


        public String getCode() {
            return code;
        }

        public OAuth2Code getCodeData() {
            return codeData;
        }

        public AuthenticatedClientSessionModel getClientSession() {
            return clientSession;
        }

        public boolean isIllegalCode() {
            return isIllegalCode;
        }

        public boolean isExpiredCode() {
            return isExpiredCode;
        }


        private ParseResult illegalCode() {
            this.isIllegalCode = true;
            return this;
        }


        private ParseResult expiredCode() {
            this.isExpiredCode = true;
            return this;
        }
    }

}
