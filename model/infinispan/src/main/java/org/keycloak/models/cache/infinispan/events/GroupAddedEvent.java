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

import org.keycloak.models.cache.infinispan.RealmCacheManager;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(GroupAddedEvent.ExternalizerImpl.class)
public class GroupAddedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String groupId;
    private String realmId;

    public static GroupAddedEvent create(String groupId, String realmId) {
        GroupAddedEvent event = new GroupAddedEvent();
        event.realmId = realmId;
        event.groupId = groupId;
        return event;
    }

    @Override
    public String getId() {
        return groupId;
    }

    @Override
    public String toString() {
        return String.format("GroupAddedEvent [ realmId=%s, groupId=%s ]", realmId, groupId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.groupQueriesInvalidations(realmId, invalidations);
    }

    public static class ExternalizerImpl implements Externalizer<GroupAddedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, GroupAddedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.groupId, output);
            MarshallUtil.marshallString(obj.realmId, output);
        }

        @Override
        public GroupAddedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public GroupAddedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            GroupAddedEvent res = new GroupAddedEvent();
            res.groupId = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
