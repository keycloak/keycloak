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
package org.keycloak.authorization.mongo.adapter;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.mongo.entities.ResourceServerEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.adapters.AbstractMongoAdapter;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceServerAdapter extends AbstractMongoAdapter<ResourceServerEntity> implements ResourceServer{

    private final ResourceServerEntity entity;

    public ResourceServerAdapter(ResourceServerEntity entity, MongoStoreInvocationContext invocationContext) {
        super(invocationContext);
        this.entity = entity;
    }

    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getClientId() {
        return getMongoEntity().getClientId();
    }

    @Override
    public boolean isAllowRemoteResourceManagement() {
        return getMongoEntity().isAllowRemoteResourceManagement();
    }

    @Override
    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        getMongoEntity().setAllowRemoteResourceManagement(allowRemoteResourceManagement);
        updateMongoEntity();
    }

    @Override
    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return getMongoEntity().getPolicyEnforcementMode();
    }

    @Override
    public void setPolicyEnforcementMode(PolicyEnforcementMode enforcementMode) {
        getMongoEntity().setPolicyEnforcementMode(enforcementMode);
        updateMongoEntity();
    }

    @Override
    protected ResourceServerEntity getMongoEntity() {
        return this.entity;
    }
}
