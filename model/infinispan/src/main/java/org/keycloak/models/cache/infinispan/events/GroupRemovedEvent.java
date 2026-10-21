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
@ProtoTypeId(Marshalling.GROUP_REMOVED_EVENT)
public class GroupRemovedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    @ProtoField(2)
    final String realmId;
    @ProtoField(3)
    final String parentId;

    public GroupRemovedEvent(String groupId, String realmId, String parentId) {
        super(groupId);
        this.realmId = Objects.requireNonNull(realmId);
        this.parentId = parentId;
    }

    @ProtoFactory
    static GroupRemovedEvent protoFactory(String id, String realmId, String parentId) {
        return new GroupRemovedEvent(id, realmId, parentId);
    }

    public static GroupRemovedEvent create(GroupModel group, String realmId) {
        return new GroupRemovedEvent(group.getId(), realmId, group.getParentId());
    }

    @Override
    public String toString() {
        return String.format("GroupRemovedEvent [ realmId=%s, groupId=%s, parentId=%s ]", realmId, getId(), parentId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.groupQueriesInvalidations(realmId, invalidations);
        realmCache.groupNameInvalidations(getId(), invalidations);
        if (parentId != null) {
            invalidations.add(parentId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GroupRemovedEvent that = (GroupRemovedEvent) o;
        return realmId.equals(that.realmId) && Objects.equals(parentId, that.parentId);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + realmId.hashCode();
        result = 31 * result + Objects.hashCode(parentId);
        return result;
    }
}
