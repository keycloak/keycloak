/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.listeners;

import java.lang.invoke.MethodHandles;

import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;

/**
 * Base class to handle expired user session.
 * <p>
 * It offloads the event creating and sending to a different thread to avoid blocking the caller.
 */
abstract class BaseUserSessionExpirationListener {

    protected static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private final KeycloakSessionFactory factory;
    private final BlockingManager blockingManager;

    BaseUserSessionExpirationListener(KeycloakSessionFactory factory, BlockingManager blockingManager) {
        this.factory = factory;
        this.blockingManager = blockingManager;
    }

    protected void sendExpirationEvent(String userSessionId, String userId, String realmId) {
        blockingManager.runBlocking(() -> doSend(userSessionId, userId, realmId), "expired-" + userSessionId);
    }

    private void doSend(String userSessionId, String userId, String realmId) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            logger.debugf("User session expired. sessionId=%s, userId=%s, realmId=%s", userSessionId, userId, realmId);

            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null) {
                return;
            }
            session.getContext().setRealm(realm);
            new EventBuilder(realm, session)
                    .session(userSessionId)
                    .user(userId)
                    .event(EventType.USER_SESSION_DELETED)
                    .detail(Details.REASON, Details.EXPIRED_DETAIL)
                    .success();
        });
    }

}
