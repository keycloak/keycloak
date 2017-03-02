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

import org.keycloak.models.*;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity.Key;

import java.util.*;
import org.infinispan.Cache;

/**
 *
 * @author hmlnarik
 */
public class InfinispanActionTokenStoreProvider implements ActionTokenStoreProvider {

    private final Cache<ActionTokenKeyModel, Set<ActionTokenValueModel>> actionKeyCache;
    private final InfinispanKeycloakTransaction tx;

    public InfinispanActionTokenStoreProvider(KeycloakSession session, Cache<ActionTokenKeyModel, Set<ActionTokenValueModel>> actionKeyCache) {
        this.actionKeyCache = actionKeyCache;
        this.tx = new InfinispanKeycloakTransaction();

        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void close() {
    }

    @Override
    public void addActionToken(ActionTokenKeyModel actionTokenKey, ActionTokenValueModel actionTokenValue) {
        if (actionTokenKey == null || actionTokenKey.getUserId()== null) {
            return;
        }

        Key key = new Key(actionTokenKey.getUserId(), actionTokenKey.getActionId());
        Set<ActionTokenValueModel> tokens = this.tx.get(actionKeyCache, key);
        if (tokens == null) {
            tokens = new HashSet<>();
        }
        tokens.add(new ActionTokenValueEntity(actionTokenValue));
        // TODO: Limit maximum number of tokens per entry?
        this.tx.put(actionKeyCache, key, tokens);
    }

    @Override
    public ActionTokenValueModel get(ActionTokenKeyModel actionTokenKey, UUID nonce) {
        if (actionTokenKey == null || actionTokenKey.getUserId() == null || actionTokenKey.getActionId() == null) {
            return null;
        }

        Key key = new Key(actionTokenKey.getUserId(), actionTokenKey.getActionId());
        Set<ActionTokenValueModel> tokens = this.actionKeyCache.get(key);
        if (tokens == null) {
            return null;
        }

        return tokens
          .stream()
          .filter(tokenEntry -> Objects.equals(tokenEntry.getActionVerificationNonce(), nonce))
          .findAny()
          .orElse(null);
    }
    
    @Override
    public ActionTokenValueModel removeActionToken(ActionTokenKeyModel actionTokenKey, UUID nonce) {
        if (actionTokenKey == null || actionTokenKey.getUserId() == null || actionTokenKey.getActionId() == null) {
            return null;
        }

        Key key = new Key(actionTokenKey.getUserId(), actionTokenKey.getActionId());

        Set<ActionTokenValueModel> tokens = this.actionKeyCache.get(key);
        if (tokens == null) {
            return null;
        }

        Optional<ActionTokenValueModel> storedToken = tokens
          .stream()
          .filter(tokenEntry -> Objects.equals(tokenEntry.getActionVerificationNonce(), nonce))
          .findAny();

        if (storedToken.isPresent()) {
            this.tx.remove(actionKeyCache, key);
        }

        return storedToken.orElse(null);
    }

}
