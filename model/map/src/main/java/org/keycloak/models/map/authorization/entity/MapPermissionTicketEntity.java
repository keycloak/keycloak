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

import org.keycloak.models.map.common.UpdatableEntity;
import java.util.Objects;

public class MapPermissionTicketEntity extends UpdatableEntity.Impl implements AbstractEntity {

    private String id;
    private String owner;
    private String requester;
    private Long createdTimestamp;
    private Long grantedTimestamp;
    private String resourceId;
    private String scopeId;
    private String resourceServerId;
    private String policyId;

    public MapPermissionTicketEntity(String id) {
        this.id = id;
    }

    public MapPermissionTicketEntity() {}

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        if (this.id != null) throw new IllegalStateException("Id cannot be changed");
        this.id = id;
        this.updated |= id != null;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.updated |= !Objects.equals(this.owner, owner);
        this.owner = owner;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.updated |= !Objects.equals(this.requester, requester);
        this.requester = requester;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.updated |= !Objects.equals(this.createdTimestamp, createdTimestamp);
        this.createdTimestamp = createdTimestamp;
    }

    public Long getGrantedTimestamp() {
        return grantedTimestamp;
    }

    public void setGrantedTimestamp(Long grantedTimestamp) {
        this.updated |= !Objects.equals(this.grantedTimestamp, grantedTimestamp);
        this.grantedTimestamp = grantedTimestamp;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.updated |= !Objects.equals(this.resourceId, resourceId);
        this.resourceId = resourceId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.updated |= !Objects.equals(this.scopeId, scopeId);
        this.scopeId = scopeId;
    }

    public String getResourceServerId() {
        return resourceServerId;
    }

    public void setResourceServerId(String resourceServerId) {
        this.updated |= !Objects.equals(this.resourceServerId, resourceServerId);
        this.resourceServerId = resourceServerId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.updated |= !Objects.equals(this.policyId, policyId);
        this.policyId = policyId;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
