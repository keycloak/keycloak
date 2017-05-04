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

/**
 * Event requesting removal of the action tokens with the given user and action regardless of nonce.
 */
public class RemoveActionTokensSpecificEvent implements ClusterEvent {

    private final String userId;
    private final String actionId;

    public RemoveActionTokensSpecificEvent(String userId, String actionId) {
        this.userId = userId;
        this.actionId = actionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getActionId() {
        return actionId;
    }

}
