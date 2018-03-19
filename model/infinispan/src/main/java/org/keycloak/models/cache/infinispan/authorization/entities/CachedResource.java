/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.cache.infinispan.authorization.entities;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedResource extends AbstractRevisioned implements InResourceServer {

    private String resourceServerId;
    private String iconUri;
    private String owner;
    private String type;
    private String name;
    private String displayName;
    private String uri;
    private Set<String> scopesIds;
    private boolean ownerManagedAccess;
    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();

    public CachedResource(Long revision, Resource resource) {
        super(revision, resource.getId());
        this.name = resource.getName();
        this.displayName = resource.getDisplayName();
        this.uri = resource.getUri();
        this.type = resource.getType();
        this.owner = resource.getOwner();
        this.iconUri = resource.getIconUri();
        this.resourceServerId = resource.getResourceServer().getId();
        this.scopesIds = resource.getScopes().stream().map(Scope::getId).collect(Collectors.toSet());
        ownerManagedAccess = resource.isOwnerManagedAccess();
        this.attributes.putAll(resource.getAttributes());
    }


    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getUri() {
        return this.uri;
    }

    public String getType() {
        return this.type;
    }

    public String getIconUri() {
        return this.iconUri;
    }

    public String getOwner() {
        return this.owner;
    }

    public boolean isOwnerManagedAccess() {
        return ownerManagedAccess;
    }

    public String getResourceServerId() {
        return this.resourceServerId;
    }

    public Set<String> getScopesIds() {
        return this.scopesIds;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }
}
