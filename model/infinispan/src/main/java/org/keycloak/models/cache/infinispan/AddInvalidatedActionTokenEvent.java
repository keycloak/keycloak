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
package org.keycloak.models.cache.infinispan;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenReducedKey;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;

/**
 * Event requesting adding of an invalidated action token.
 */
public class AddInvalidatedActionTokenEvent implements ClusterEvent {

    private final ActionTokenReducedKey key;
    private final int expirationInSecs;
    private final ActionTokenValueEntity tokenValue;

    public AddInvalidatedActionTokenEvent(ActionTokenReducedKey key, int expirationInSecs, ActionTokenValueEntity tokenValue) {
        this.key = key;
        this.expirationInSecs = expirationInSecs;
        this.tokenValue = tokenValue;
    }

    public ActionTokenReducedKey getKey() {
        return key;
    }

    public int getExpirationInSecs() {
        return expirationInSecs;
    }

    public ActionTokenValueEntity getTokenValue() {
        return tokenValue;
    }

}
