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

package org.keycloak.models.cache.infinispan.authorization.entities;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedPermissionTicket extends AbstractRevisioned implements InResourceServer {

    private final String requester;
    private String owner;
    private String resourceServerId;
    private String resourceId;
    private String scopeId;
    private boolean granted;
    private Long createdTimestamp;
    private Long grantedTimestamp;
    private String policy;

    public CachedPermissionTicket(Long revision, PermissionTicket permissionTicket) {
        super(revision, permissionTicket.getId());
        this.owner = permissionTicket.getOwner();
        requester = permissionTicket.getRequester();
        this.resourceServerId = permissionTicket.getResourceServer().getId();
        this.resourceId = permissionTicket.getResource().getId();
        if (permissionTicket.getScope() != null) {
            this.scopeId = permissionTicket.getScope().getId();
        }
        this.granted = permissionTicket.isGranted();
        createdTimestamp = permissionTicket.getCreatedTimestamp();
        grantedTimestamp = permissionTicket.getGrantedTimestamp();
        Policy policy = permissionTicket.getPolicy();
        if (policy != null) {
            this.policy = policy.getId();
        }
    }

    public String getOwner() {
        return owner;
    }

    public String getRequester() {
        return requester;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public boolean isGranted() {
        return granted;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public Long getGrantedTimestamp() {
        return grantedTimestamp;
    }

    public String getResourceServerId() {
        return this.resourceServerId;
    }

    public String getPolicy() {
        return policy;
    }
}
