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

package org.keycloak.authorization.mongo.entities;

import org.keycloak.authorization.model.Scope;
import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.AbstractIdentifiableEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@MongoCollection(collectionName = "resources")
public class ResourceEntity extends AbstractIdentifiableEntity implements MongoIdentifiableEntity {

    private String name;

    private String uri;

    private String type;

    private String iconUri;

    private String owner;

    private String resourceServerId;

    private List<String> scopes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getScopes() {
        return this.scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public String getResourceServerId() {
        return resourceServerId;
    }

    public void setResourceServerId(String resourceServerId) {
        this.resourceServerId = resourceServerId;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void updateScopes(Set<Scope> toUpdate) {
        for (Scope scope : toUpdate) {
            boolean hasScope = false;

            for (String existingScope : this.scopes) {
                if (existingScope.equals(scope.getId())) {
                    hasScope = true;
                }
            }

            if (!hasScope) {
                this.scopes.add(scope.getId());
            }
        }

        for (String scopeId : new HashSet<String>(this.scopes)) {
            boolean hasScope = false;

            for (Scope scope : toUpdate) {
                if (scopeId.equals(scope.getId())) {
                    hasScope = true;
                }
            }

            if (!hasScope) {
                this.scopes.remove(scopeId);
            }
        }
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext invocationContext) {

    }
}
