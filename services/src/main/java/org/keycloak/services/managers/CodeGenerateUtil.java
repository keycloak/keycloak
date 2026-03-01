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
import java.util.function.Supplier;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import org.jboss.logging.Logger;

/**
 * TODO: Remove this and probably also ClientSessionParser. It's unnecessary genericity and abstraction, which is not needed anymore when clientSessionModel was fully removed.
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
    }



    static <CS extends CommonClientSessionModel> ClientSessionParser<CS> getParser(Class<CS> clientSessionClass) {
        for (var entry : PARSERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(clientSessionClass)) {
                return entry.getValue().get();
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

        String getClientNote(CS clientSession, String noteKey);

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
                String actionId = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
                authSession.setAuthNote(ACTIVE_CODE, actionId);

                // enlist a transaction that ensures the code is set in the auth session in case the main transaction is rolled back.
                session.getTransactionManager().enlist(new RollbackDrivenTransaction(session.getKeycloakSessionFactory(), currentSession -> {
                    final RootAuthenticationSessionModel rootAuthenticationSession = currentSession.authenticationSessions()
                            .getRootAuthenticationSession(authSession.getRealm(), authSession.getParentSession().getId());
                    AuthenticationSessionModel authenticationSession = rootAuthenticationSession == null ? null : rootAuthenticationSession
                            .getAuthenticationSession(authSession.getClient(), authSession.getTabId());
                    if (authenticationSession != null) {
                        authenticationSession.setAuthNote(ACTIVE_CODE, actionId);
                    }
                }));
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
            // enlist a transaction that ensures the code is removed in case the main transaction is rolled back.
            session.getTransactionManager().enlist(new RollbackDrivenTransaction(session.getKeycloakSessionFactory(), currentSession -> {
                AuthenticationSessionModel authenticationSession = currentSession.authenticationSessions()
                        .getRootAuthenticationSession(authSession.getRealm(), authSession.getParentSession().getId())
                        .getAuthenticationSession(authSession.getClient(), authSession.getTabId());
                authenticationSession.removeAuthNote(ACTIVE_CODE);
            }));

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

        @Override
        public String getClientNote(AuthenticationSessionModel clientSession, String noteKey) {
            return clientSession.getClientNote(noteKey);
        }
    }

    /**
     * A {@link KeycloakTransaction} that runs a task only when {@link #rollback()} is called.
     */
    private static class RollbackDrivenTransaction implements KeycloakTransaction {

        private final KeycloakSessionFactory factory;
        private final KeycloakSessionTask task;

        RollbackDrivenTransaction(final KeycloakSessionFactory factory, final KeycloakSessionTask task) {
            this.factory = factory;
            this.task = task;
        }

        @Override
        public void begin() {
            // no-op - this tx doesn't participate in the regular transaction flow, only when rollback is triggered.
        }

        @Override
        public void commit() {
            // no-op - this tx doesn't participate in the regular transaction flow, only when rollback is triggered.
        }

        @Override
        public void rollback() {
            KeycloakModelUtils.runJobInTransaction(this.factory, this.task);
        }

        @Override
        public void setRollbackOnly() {
            // no-op - this tx doesn't participate in the regular transaction flow, only when rollback is triggered.
        }

        @Override
        public boolean getRollbackOnly() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }
}
