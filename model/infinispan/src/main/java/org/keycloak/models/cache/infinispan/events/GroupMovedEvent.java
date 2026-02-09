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
import org.keycloak.models.GroupModel;
import org.keycloak.models.cache.infinispan.RealmCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.GROUP_MOVED_EVENT)
public class GroupMovedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    @ProtoField(2)
    final String newParentId; // null if moving to top-level
    @ProtoField(3)
    final String oldParentId; // null if moving from top-level
    @ProtoField(4)
    final String realmId;

    private GroupMovedEvent(String groupId, String newParentId, String oldParentId, String realmId) {
        super(groupId);
        this.newParentId = newParentId;
        this.oldParentId = oldParentId;
        this.realmId = Objects.requireNonNull(realmId);
    }

    @ProtoFactory
    static GroupMovedEvent protoFactory(String id, String newParentId, String oldParentId, String realmId) {
        return new GroupMovedEvent(id, newParentId, oldParentId, realmId);
    }

    public static GroupMovedEvent create(GroupModel group, GroupModel toParent, String realmId) {
        return new GroupMovedEvent(group.getId(), group.getId(), toParent == null ? null : toParent.getId(), realmId);
    }

    @Override
    public String toString() {
        return String.format("GroupMovedEvent [ realmId=%s, groupId=%s, newParentId=%s, oldParentId=%s ]", realmId, getId(), newParentId, oldParentId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.groupQueriesInvalidations(realmId, invalidations);
        realmCache.groupNameInvalidations(getId(), invalidations);
        if (newParentId != null) {
            invalidations.add(newParentId);
        }
        if (oldParentId != null) {
            invalidations.add(oldParentId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GroupMovedEvent that = (GroupMovedEvent) o;
        return Objects.equals(newParentId, that.newParentId) &&
                Objects.equals(oldParentId, that.oldParentId) &&
                realmId.equals(that.realmId);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(newParentId);
        result = 31 * result + Objects.hashCode(oldParentId);
        result = 31 * result + realmId.hashCode();
        return result;
    }
}
