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
package org.keycloak.models.sessions.infinispan;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.*;

import org.keycloak.models.cache.infinispan.AddInvalidatedActionTokenEvent;
import org.keycloak.models.cache.infinispan.RemoveActionTokensSpecificEvent;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenReducedKey;
import java.util.*;
import org.infinispan.Cache;

/**
 *
 * @author hmlnarik
 */
public class InfinispanActionTokenStoreProvider implements ActionTokenStoreProvider {

    private final Cache<ActionTokenReducedKey, ActionTokenValueEntity> actionKeyCache;
    private final InfinispanKeycloakTransaction tx;
    private final KeycloakSession session;

    public InfinispanActionTokenStoreProvider(KeycloakSession session, Cache<ActionTokenReducedKey, ActionTokenValueEntity> actionKeyCache) {
        this.session = session;
        this.actionKeyCache = actionKeyCache;
        this.tx = new InfinispanKeycloakTransaction();

        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void close() {
    }

    @Override
    public void put(ActionTokenKeyModel key, Map<String, String> notes) {
        if (key == null || key.getUserId() == null || key.getActionId() == null) {
            return;
        }

        ActionTokenReducedKey tokenKey = new ActionTokenReducedKey(key.getUserId(), key.getActionId(), key.getActionVerificationNonce());
        ActionTokenValueEntity tokenValue = new ActionTokenValueEntity(notes);

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        this.tx.notify(cluster, InfinispanActionTokenStoreProviderFactory.ACTION_TOKEN_EVENTS, new AddInvalidatedActionTokenEvent(tokenKey, key.getExpiration(), tokenValue), false);
    }

    @Override
    public ActionTokenValueModel get(ActionTokenKeyModel actionTokenKey) {
        if (actionTokenKey == null || actionTokenKey.getUserId() == null || actionTokenKey.getActionId() == null) {
            return null;
        }

        ActionTokenReducedKey key = new ActionTokenReducedKey(actionTokenKey.getUserId(), actionTokenKey.getActionId(), actionTokenKey.getActionVerificationNonce());
        return this.actionKeyCache.getAdvancedCache().get(key);
    }
    
    @Override
    public ActionTokenValueModel remove(ActionTokenKeyModel actionTokenKey) {
        if (actionTokenKey == null || actionTokenKey.getUserId() == null || actionTokenKey.getActionId() == null) {
            return null;
        }

        ActionTokenReducedKey key = new ActionTokenReducedKey(actionTokenKey.getUserId(), actionTokenKey.getActionId(), actionTokenKey.getActionVerificationNonce());
        ActionTokenValueEntity value = this.actionKeyCache.get(key);

        if (value != null) {
            this.tx.remove(actionKeyCache, key);
        }

        return value;
    }

    public void removeAll(String userId, String actionId) {
        if (userId == null || actionId == null) {
            return;
        }

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        this.tx.notify(cluster, InfinispanActionTokenStoreProviderFactory.ACTION_TOKEN_EVENTS, new RemoveActionTokensSpecificEvent(userId, actionId), false);
    }
}
