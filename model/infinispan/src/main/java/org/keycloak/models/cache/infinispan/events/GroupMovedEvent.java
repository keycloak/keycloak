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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(GroupMovedEvent.ExternalizerImpl.class)
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

    public static class ExternalizerImpl implements Externalizer<GroupMovedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, GroupMovedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.groupId, output);
            MarshallUtil.marshallString(obj.newParentId, output);
            MarshallUtil.marshallString(obj.oldParentId, output);
            MarshallUtil.marshallString(obj.realmId, output);
        }

        @Override
        public GroupMovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public GroupMovedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            GroupMovedEvent res = new GroupMovedEvent();
            res.groupId = MarshallUtil.unmarshallString(input);
            res.newParentId = MarshallUtil.unmarshallString(input);
            res.oldParentId = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
