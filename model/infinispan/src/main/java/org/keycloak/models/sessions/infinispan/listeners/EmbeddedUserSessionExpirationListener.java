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

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.event.CacheEntryExpiredEvent;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * A listener for embedded Infinispan caches.
 * <p>
 * It listens to the {@link CacheEntryExpired} events for user sessions.
 */
@Listener(primaryOnly = true)
public class EmbeddedUserSessionExpirationListener extends BaseUserSessionExpirationListener {

    public EmbeddedUserSessionExpirationListener(KeycloakSessionFactory factory, BlockingManager blockingManager) {
        super(factory, blockingManager);
    }

    @CacheEntryExpired
    public void onSessionExpired(CacheEntryExpiredEvent<?, SessionEntityWrapper<UserSessionEntity>> event) {
        UserSessionEntity entity = event.getValue().getEntity();
        sendExpirationEvent(entity.getId(), entity.getUser(), entity.getRealmId());
    }
}
