/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ResourceWrapper implements Resource {

    private final Resource resource;
    private final String name;
    private final ResourceServer resourceServer;
    private final Set<Scope> scopes;
    private final String id;

    public ResourceWrapper(String name, Set<Scope> scopes, ResourceServer resourceServer) {
        this(null, name, scopes, resourceServer);
    }

    public ResourceWrapper(String id, String name, Set<Scope> scopes, ResourceServer resourceServer) {
        this.id = id;
        this.name = name;
        this.scopes = scopes;
        this.resourceServer = resourceServer;
        this.resource = null;
    }

    @Override
    public String getId() {
        return Optional.ofNullable(resource).map(Resource::getId).orElse(id);
    }

    @Override
    public String getName() {
        return Optional.ofNullable(resource).map(Resource::getName).orElse(name);
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public String getDisplayName() {
        return Optional.ofNullable(resource).map(Resource::getDisplayName).orElse(null);
    }

    @Override
    public void setDisplayName(String name) {
    }

    @Override
    public Set<String> getUris() {
        return Optional.ofNullable(resource).map(Resource::getUris).orElse(Set.of());
    }

    @Override
    public void updateUris(Set<String> uri) {
    }

    @Override
    public String getType() {
        return Optional.ofNullable(resource).map(Resource::getType).orElse(null);
    }

    @Override
    public void setType(String type) {
    }

    @Override
    public List<Scope> getScopes() {
        return Optional.ofNullable(resource).map(Resource::getScopes).orElse(scopes.stream().toList());
    }

    @Override
    public String getIconUri() {
        return Optional.ofNullable(resource).map(Resource::getIconUri).orElse(null);
    }

    @Override
    public void setIconUri(String iconUri) {
    }

    @Override
    public ResourceServer getResourceServer() {
        return resourceServer;
    }

    @Override
    public String getOwner() {
        return Optional.ofNullable(resource).map(Resource::getOwner).orElse(resourceServer.getId());
    }

    @Override
    public boolean isOwnerManagedAccess() {
        return Optional.ofNullable(resource).map(Resource::isOwnerManagedAccess).orElse(false);
    }

    @Override
    public void setOwnerManagedAccess(boolean ownerManagedAccess) {
    }

    @Override
    public void updateScopes(Set<Scope> scopes) {
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return Optional.ofNullable(resource).map(Resource::getAttributes).orElse(Map.of());
    }

    @Override
    public String getSingleAttribute(String name) {
        return getAttributes().getOrDefault(name, List.of()).stream().findFirst().orElse(null);
    }

    @Override
    public List<String> getAttribute(String name) {
        return getAttributes().getOrDefault(name, List.of());
    }

    @Override
    public void setAttribute(String name, List<String> values) {
    }

    @Override
    public void removeAttribute(String name) {
    }
}
