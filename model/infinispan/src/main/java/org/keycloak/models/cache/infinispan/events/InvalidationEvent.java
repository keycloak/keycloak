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

package org.keycloak.models.cache.infinispan.events;

import java.util.Objects;

import org.keycloak.cluster.ClusterEvent;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class InvalidationEvent implements ClusterEvent {

    private final String id;

    protected InvalidationEvent(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @ProtoField(1)
    public final String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() * 13 + getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!obj.getClass().equals(this.getClass())) return false;

        InvalidationEvent that = (InvalidationEvent) obj;
        return that.getId().equals(getId());
    }
}
