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

package org.keycloak.models.map.authorization.adapter;


import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

public abstract class MapResourceServerAdapter<K> extends AbstractResourceServerModel<MapResourceServerEntity<K>> {

    public MapResourceServerAdapter(MapResourceServerEntity<K> entity, StoreFactory storeFactory) {
        super(entity, storeFactory);
    }

    @Override
    public boolean isAllowRemoteResourceManagement() {
        return entity.isAllowRemoteResourceManagement();
    }

    @Override
    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        throwExceptionIfReadonly();
        entity.setAllowRemoteResourceManagement(allowRemoteResourceManagement);
    }

    @Override
    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return entity.getPolicyEnforcementMode();
    }

    @Override
    public void setPolicyEnforcementMode(PolicyEnforcementMode enforcementMode) {
        throwExceptionIfReadonly();
        entity.setPolicyEnforcementMode(enforcementMode);
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        throwExceptionIfReadonly();
        entity.setDecisionStrategy(decisionStrategy);
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        return entity.getDecisionStrategy();
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
