/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.cache.infinispan.authorization.events;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

import org.infinispan.protostream.annotations.ProtoField;

abstract class BasePolicyEvent extends InvalidationEvent implements AuthorizationCacheInvalidationEvent {

    @ProtoField(2)
    final String name;
    @ProtoField(value = 3, collectionImplementation = HashSet.class)
    final Set<String> resources;
    @ProtoField(value = 4, collectionImplementation = HashSet.class)
    final Set<String> resourceTypes;
    @ProtoField(value = 5, collectionImplementation = HashSet.class)
    final Set<String> scopes;
    @ProtoField(6)
    final String serverId;

    BasePolicyEvent(String id, String name, Set<String> resources, Set<String> resourceTypes, Set<String> scopes, String serverId) {
        super(id);
        this.name = Objects.requireNonNull(name);
        this.resources = resources;
        this.resourceTypes = resourceTypes;
        this.scopes = scopes;
        this.serverId = Objects.requireNonNull(serverId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BasePolicyEvent that = (BasePolicyEvent) o;
        return name.equals(that.name) &&
                Objects.equals(resources, that.resources) &&
                Objects.equals(resourceTypes, that.resourceTypes) &&
                Objects.equals(scopes, that.scopes) &&
                serverId.equals(that.serverId);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + Objects.hashCode(resources);
        result = 31 * result + Objects.hashCode(resourceTypes);
        result = 31 * result + Objects.hashCode(scopes);
        result = 31 * result + serverId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s [id=%s, name=%s]", getClass().getSimpleName(), getId(), name);
    }
}
