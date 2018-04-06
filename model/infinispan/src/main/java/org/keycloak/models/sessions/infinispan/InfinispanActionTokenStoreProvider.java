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

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.*;

import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenReducedKey;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.infinispan.Cache;

/**
 *
 * @author hmlnarik
 */
public class InfinispanActionTokenStoreProvider implements ActionTokenStoreProvider {

    private static final Logger LOG = Logger.getLogger(InfinispanActionTokenStoreProvider.class);

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

        LOG.debugf("Adding used action token to actionTokens cache: %s", tokenKey.toString());

        this.tx.put(actionKeyCache, tokenKey, tokenValue, key.getExpiration() - Time.currentTime(), TimeUnit.SECONDS);
    }

    @Override
    public ActionTokenValueModel get(ActionTokenKeyModel actionTokenKey) {
        if (actionTokenKey == null || actionTokenKey.getUserId() == null || actionTokenKey.getActionId() == null) {
            return null;
        }

        ActionTokenReducedKey key = new ActionTokenReducedKey(actionTokenKey.getUserId(), actionTokenKey.getActionId(), actionTokenKey.getActionVerificationNonce());

        ActionTokenValueModel value = this.actionKeyCache.getAdvancedCache().get(key);
        if (value == null) {
            LOG.debugf("Not found any value in actionTokens cache for key: %s", key.toString());
        } else {
            LOG.debugf("Found value in actionTokens cache for key: %s", key.toString());
        }

        return value;
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
}
