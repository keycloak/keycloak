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

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import javax.crypto.SecretKey;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.CodeJWT;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

/**
 * TODO: Remove this and probably also ClientSessionParser. It's uneccessary genericity and abstraction, which is not needed anymore when clientSessionModel was fully removed.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class CodeGenerateUtil {

    private static final Logger logger = Logger.getLogger(CodeGenerateUtil.class);

    private static final String ACTIVE_CODE = "active_code";

    private static final Map<Class<? extends CommonClientSessionModel>, Supplier<ClientSessionParser>> PARSERS = new HashMap<>();

    static {
        PARSERS.put(AuthenticationSessionModel.class, () -> {
            return new AuthenticationSessionModelParser();
        });

        PARSERS.put(AuthenticatedClientSessionModel.class, () -> {
            return new AuthenticatedClientSessionModelParser();
        });
    }



    static <CS extends CommonClientSessionModel> ClientSessionParser<CS> getParser(Class<CS> clientSessionClass) {
        for (Class<?> c : PARSERS.keySet()) {
            if (c.isAssignableFrom(clientSessionClass)) {
                return PARSERS.get(c).get();
            }
        }
        return null;
    }


    interface ClientSessionParser<CS extends CommonClientSessionModel> {

        CS parseSession(String code, String tabId, KeycloakSession session, RealmModel realm, ClientModel client, EventBuilder event);

        String retrieveCode(KeycloakSession session, CS clientSession);

        void removeExpiredSession(KeycloakSession session, CS clientSession);

        boolean verifyCode(KeycloakSession session, String code, CS clientSession);

        boolean isExpired(KeycloakSession session, String code, CS clientSession);

        int getTimestamp(CS clientSession);
        void setTimestamp(CS clientSession, int timestamp);

    }


    // IMPLEMENTATIONS


    private static class AuthenticationSessionModelParser implements ClientSessionParser<AuthenticationSessionModel> {

        @Override
        public AuthenticationSessionModel parseSession(String code, String tabId, KeycloakSession session, RealmModel realm, ClientModel client, EventBuilder event) {
            // Read authSessionID from cookie. Code is ignored for now
            return new AuthenticationSessionManager(session).getCurrentAuthenticationSession(realm, client, tabId);
        }

        @Override
        public String retrieveCode(KeycloakSession session, AuthenticationSessionModel authSession) {
            String nextCode = authSession.getAuthNote(ACTIVE_CODE);
            if (nextCode == null) {
                String actionId = Base64Url.encode(KeycloakModelUtils.generateSecret());
                authSession.setAuthNote(ACTIVE_CODE, actionId);
                nextCode = actionId;
            } else {
                logger.debug("Code already generated for authentication session, using same code");
            }

            return nextCode;
        }


        @Override
        public void removeExpiredSession(KeycloakSession session, AuthenticationSessionModel clientSession) {
            new AuthenticationSessionManager(session).removeAuthenticationSession(clientSession.getRealm(), clientSession, true);
        }


        @Override
        public boolean verifyCode(KeycloakSession session, String code, AuthenticationSessionModel authSession) {
            String activeCode = authSession.getAuthNote(ACTIVE_CODE);
            if (activeCode == null) {
                logger.debug("Active code not found in authentication session");
                return false;
            }

            authSession.removeAuthNote(ACTIVE_CODE);

            return MessageDigest.isEqual(code.getBytes(), activeCode.getBytes());
        }


        @Override
        public boolean isExpired(KeycloakSession session, String code, AuthenticationSessionModel clientSession) {
            return false;
        }

        @Override
        public int getTimestamp(AuthenticationSessionModel clientSession) {
            return clientSession.getParentSession().getTimestamp();
        }

        @Override
        public void setTimestamp(AuthenticationSessionModel clientSession, int timestamp) {
            clientSession.getParentSession().setTimestamp(timestamp);
        }
    }


    private static class AuthenticatedClientSessionModelParser implements ClientSessionParser<AuthenticatedClientSessionModel> {

        private CodeJWT codeJWT;

        @Override
        public AuthenticatedClientSessionModel parseSession(String code, String tabId, KeycloakSession session, RealmModel realm, ClientModel client, EventBuilder event) {
            SecretKey aesKey = session.keys().getActiveAesKey(realm).getSecretKey();
            SecretKey hmacKey = session.keys().getActiveHmacKey(realm).getSecretKey();

            try {
                codeJWT = TokenUtil.jweDirectVerifyAndDecode(aesKey, hmacKey, code, CodeJWT.class);
            } catch (JWEException jweException) {
                logger.error("Exception during JWE Verification or decode", jweException);
                return null;
            }

            event.detail(Details.CODE_ID, codeJWT.getUserSessionId());
            event.session(codeJWT.getUserSessionId());

            UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, codeJWT.getUserSessionId(), codeJWT.getIssuedFor());
            if (userSession == null) {
                // TODO:mposolda Temporary workaround needed to track if code is invalid or was already used. Will be good to remove once used OAuth codes are tracked through one-time cache
                userSession = session.sessions().getUserSession(realm, codeJWT.getUserSessionId());
                if (userSession == null) {
                    return null;
                }
            }

            return userSession.getAuthenticatedClientSessionByClient(codeJWT.getIssuedFor());

        }


        @Override
        public String retrieveCode(KeycloakSession session, AuthenticatedClientSessionModel clientSession) {
            String actionId = KeycloakModelUtils.generateId();

            CodeJWT codeJWT = new CodeJWT();
            codeJWT.id(actionId);
            codeJWT.issuedFor(clientSession.getClient().getId());
            codeJWT.userSessionId(clientSession.getUserSession().getId());

            RealmModel realm = clientSession.getRealm();

            int issuedAt = Time.currentTime();
            codeJWT.issuedAt(issuedAt);
            codeJWT.expiration(issuedAt + realm.getAccessCodeLifespan());

            SecretKey aesKey = session.keys().getActiveAesKey(realm).getSecretKey();
            SecretKey hmacKey = session.keys().getActiveHmacKey(realm).getSecretKey();

            if (logger.isTraceEnabled()) {
                logger.tracef("Using AES key of length '%d' bytes and HMAC key of length '%d' bytes . Client: '%s', User Session: '%s'", aesKey.getEncoded().length,
                        hmacKey.getEncoded().length, clientSession.getClient().getClientId(), clientSession.getUserSession().getId());
            }

            try {
                return TokenUtil.jweDirectEncode(aesKey, hmacKey, codeJWT);
            } catch (JWEException jweEx) {
                throw new RuntimeException(jweEx);
            }
        }


        @Override
        public boolean verifyCode(KeycloakSession session, String code, AuthenticatedClientSessionModel clientSession) {
            if (codeJWT == null) {
                throw new IllegalStateException("Illegal use. codeJWT not yet set");
            }

            UUID codeId = UUID.fromString(codeJWT.getId());
            CodeToTokenStoreProvider singleUseCache = session.getProvider(CodeToTokenStoreProvider.class);

            if (singleUseCache.putIfAbsent(codeId)) {

                if (logger.isTraceEnabled()) {
                    logger.tracef("Added code '%s' to single-use cache. User session: %s, client: %s", codeJWT.getId(), codeJWT.getUserSessionId(), codeJWT.getIssuedFor());
                }

                return true;
            } else {
                logger.warnf("Code '%s' already used for userSession '%s' and client '%s'.", codeJWT.getId(), codeJWT.getUserSessionId(), codeJWT.getIssuedFor());
                return false;
            }
        }


        @Override
        public void removeExpiredSession(KeycloakSession session, AuthenticatedClientSessionModel clientSession) {
            throw new IllegalStateException("Not yet implemented");
        }


        @Override
        public boolean isExpired(KeycloakSession session, String code, AuthenticatedClientSessionModel clientSession) {
            return !codeJWT.isActive();
        }

        @Override
        public int getTimestamp(AuthenticatedClientSessionModel clientSession) {
            return clientSession.getTimestamp();
        }

        @Override
        public void setTimestamp(AuthenticatedClientSessionModel clientSession, int timestamp) {
            clientSession.setTimestamp(timestamp);
        }
    }


}
