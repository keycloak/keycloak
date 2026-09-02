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

abstract class BaseResourceEvent extends InvalidationEvent implements AuthorizationCacheInvalidationEvent {

    @ProtoField(2)
    final String name;
    @ProtoField(3)
    final String owner;
    @ProtoField(4)
    final String serverId;
    @ProtoField(5)
    final String type;
    @ProtoField(value = 6, collectionImplementation = HashSet.class)
    final Set<String> uris;
    @ProtoField(value = 7, collectionImplementation = HashSet.class)
    final Set<String> scopes;

    BaseResourceEvent(String id, String name, String owner, String serverId, String type, Set<String> uris, Set<String> scopes) {
        super(id);
        this.name = Objects.requireNonNull(name);
        this.owner = Objects.requireNonNull(owner);
        this.serverId = Objects.requireNonNull(serverId);
        this.type = type;
        this.uris = uris;
        this.scopes = scopes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseResourceEvent that = (BaseResourceEvent) o;
        return name.equals(that.name) &&
                owner.equals(that.owner) &&
                serverId.equals(that.serverId) &&
                Objects.equals(type, that.type) &&
                Objects.equals(uris, that.uris) &&
                Objects.equals(scopes, that.scopes);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + owner.hashCode();
        result = 31 * result + serverId.hashCode();
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(uris);
        result = 31 * result + Objects.hashCode(scopes);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s [ id=%s, name=%s]", getClass().getSimpleName(), getId(), name);
    }
}
