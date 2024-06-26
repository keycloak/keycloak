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

package org.keycloak.models.cache.infinispan.events;

import java.util.Objects;

import org.infinispan.protostream.annotations.ProtoField;

abstract class BaseRealmEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    @ProtoField(2)
    final String realmName;

    BaseRealmEvent(String realmId, String realmName) {
        super(realmId);
        this.realmName = Objects.requireNonNull(realmName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseRealmEvent that = (BaseRealmEvent) o;
        return realmName.equals(that.realmName);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + realmName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s [ realmId=%s, realmName=%s ]", getClass().getSimpleName(), getId(), realmName);
    }
}
