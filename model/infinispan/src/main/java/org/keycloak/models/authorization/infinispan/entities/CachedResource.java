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

package org.keycloak.models.authorization.infinispan.entities;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedResource implements Resource, Serializable {

    private static final long serialVersionUID = -6886179034626995165L;

    private final String id;
    private String resourceServerId;
    private String iconUri;
    private String owner;
    private String type;
    private String name;
    private String uri;
    private Set<String> scopesIds;

    public CachedResource(Resource resource) {
        this.id = resource.getId();
        this.name = resource.getName();
        this.uri = resource.getUri();
        this.type = resource.getType();
        this.owner = resource.getOwner();
        this.iconUri = resource.getIconUri();
        this.resourceServerId = resource.getResourceServer().getId();
        this.scopesIds = resource.getScopes().stream().map(Scope::getId).collect(Collectors.toSet());
    }

    public CachedResource(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUri() {
        return this.uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<Scope> getScopes() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getIconUri() {
        return this.iconUri;
    }

    @Override
    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    @Override
    public ResourceServer getResourceServer() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getOwner() {
        return this.owner;
    }

    @Override
    public void updateScopes(Set<Scope> scopes) {
        this.scopesIds.clear();
        this.scopesIds.addAll(scopes.stream().map(Scope::getId).collect(Collectors.toSet()));
    }

    public String getResourceServerId() {
        return this.resourceServerId;
    }

    public Set<String> getScopesIds() {
        return this.scopesIds;
    }
}
