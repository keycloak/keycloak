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
@SerializeWith(GroupRemovedEvent.ExternalizerImpl.class)
public class GroupRemovedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String groupId;
    private String parentId;
    private String realmId;

    public static GroupRemovedEvent create(GroupModel group, String realmId) {
        GroupRemovedEvent event = new GroupRemovedEvent();
        event.realmId = realmId;
        event.groupId = group.getId();
        event.parentId = group.getParentId();
        return event;
    }

    @Override
    public String getId() {
        return groupId;
    }

    @Override
    public String toString() {
        return String.format("GroupRemovedEvent [ realmId=%s, groupId=%s, parentId=%s ]", realmId, groupId, parentId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.groupQueriesInvalidations(realmId, invalidations);
        if (parentId != null) {
            invalidations.add(parentId);
        }
    }

    public static class ExternalizerImpl implements Externalizer<GroupRemovedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, GroupRemovedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.realmId, output);
            MarshallUtil.marshallString(obj.groupId, output);
            MarshallUtil.marshallString(obj.parentId, output);
        }

        @Override
        public GroupRemovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public GroupRemovedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            GroupRemovedEvent res = new GroupRemovedEvent();
            res.realmId = MarshallUtil.unmarshallString(input);
            res.groupId = MarshallUtil.unmarshallString(input);
            res.parentId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
