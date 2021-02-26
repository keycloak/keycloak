/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.authorization.entity;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

import java.util.Objects;

public class MapResourceServerEntity<K> implements AbstractEntity<K> {

    private final K id;
    private boolean updated = false;

    private boolean allowRemoteResourceManagement;
    private PolicyEnforcementMode policyEnforcementMode = PolicyEnforcementMode.ENFORCING;
    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;

    public MapResourceServerEntity(K id) {
        this.id = id;
    }

    public MapResourceServerEntity() {
        this.id = null;
    }

    @Override
    public K getId() {
        return id;
    }

    public boolean isAllowRemoteResourceManagement() {
        return allowRemoteResourceManagement;
    }

    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        this.updated |= this.allowRemoteResourceManagement != allowRemoteResourceManagement;
        this.allowRemoteResourceManagement = allowRemoteResourceManagement;
    }

    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return policyEnforcementMode;
    }

    public void setPolicyEnforcementMode(PolicyEnforcementMode policyEnforcementMode) {
        this.updated |= !Objects.equals(this.policyEnforcementMode, policyEnforcementMode);
        this.policyEnforcementMode = policyEnforcementMode;
    }

    public DecisionStrategy getDecisionStrategy() {
        return decisionStrategy;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.updated |= !Objects.equals(this.decisionStrategy, decisionStrategy);
        this.decisionStrategy = decisionStrategy;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
