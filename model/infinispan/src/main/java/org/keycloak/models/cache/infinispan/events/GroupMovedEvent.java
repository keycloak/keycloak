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

import java.util.Set;

import org.keycloak.models.GroupModel;
import org.keycloak.models.cache.infinispan.RealmCacheManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupMovedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String groupId;
    private String newParentId; // null if moving to top-level
    private String oldParentId; // null if moving from top-level
    private String realmId;

    public static GroupMovedEvent create(GroupModel group, GroupModel toParent, String realmId) {
        GroupMovedEvent event = new GroupMovedEvent();
        event.realmId = realmId;
        event.groupId = group.getId();
        event.oldParentId = group.getParentId();
        event.newParentId = toParent==null ? null : toParent.getId();
        return event;
    }

    @Override
    public String getId() {
        return groupId;
    }

    @Override
    public String toString() {
        return String.format("GroupMovedEvent [ realmId=%s, groupId=%s, newParentId=%s, oldParentId=%s ]", realmId, groupId, newParentId, oldParentId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.groupQueriesInvalidations(realmId, invalidations);
        if (newParentId != null) {
            invalidations.add(newParentId);
        }
        if (oldParentId != null) {
            invalidations.add(oldParentId);
        }
    }
}
