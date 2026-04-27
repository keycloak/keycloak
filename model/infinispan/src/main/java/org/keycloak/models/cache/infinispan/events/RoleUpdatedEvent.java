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
import java.util.Set;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.RealmCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.ROLE_UPDATED_EVENT)
public class RoleUpdatedEvent extends BaseRoleEvent {

    @ProtoField(3)
    final String roleName;

    @ProtoFactory
    RoleUpdatedEvent(String id, String containerId, String roleName) {
        super(id, containerId);
        this.roleName = Objects.requireNonNull(roleName);
    }

    public static RoleUpdatedEvent create(String roleId, String roleName, String containerId) {
        return new RoleUpdatedEvent(roleId, containerId, roleName);
    }

    @Override
    public String toString() {
        return String.format("RoleUpdatedEvent [ roleId=%s, roleName=%s, containerId=%s ]", getId(), roleName, containerId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.roleUpdated(containerId, roleName, invalidations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RoleUpdatedEvent that = (RoleUpdatedEvent) o;
        return roleName.equals(that.roleName);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + roleName.hashCode();
        return result;
    }
}
