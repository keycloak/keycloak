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

import java.util.Objects;

import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

import org.infinispan.protostream.annotations.ProtoField;

abstract class BaseScopeEvent extends InvalidationEvent implements AuthorizationCacheInvalidationEvent {

    @ProtoField(2)
    final String name;
    @ProtoField(3)
    final String serverId;

    BaseScopeEvent(String id, String name, String serverId) {
        super(id);
        this.name = Objects.requireNonNull(name);
        this.serverId = Objects.requireNonNull(serverId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseScopeEvent that = (BaseScopeEvent) o;
        return name.equals(that.name) &&
                serverId.equals(that.serverId);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + serverId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s [ id=%s, name=%s ]", getClass(), getId(), name);
    }
}
