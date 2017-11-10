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
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(RoleRemovedEvent.ExternalizerImpl.class)
public class RoleRemovedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String roleId;
    private String roleName;
    private String containerId;

    public static RoleRemovedEvent create(String roleId, String roleName, String containerId) {
        RoleRemovedEvent event = new RoleRemovedEvent();
        event.roleId = roleId;
        event.roleName = roleName;
        event.containerId = containerId;
        return event;
    }

    @Override
    public String getId() {
        return roleId;
    }

    @Override
    public String toString() {
        return String.format("RoleRemovedEvent [ roleId=%s, containerId=%s ]", roleId, containerId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.roleRemoval(roleId, roleName, containerId, invalidations);
    }

    public static class ExternalizerImpl implements Externalizer<RoleRemovedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, RoleRemovedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.roleId, output);
            MarshallUtil.marshallString(obj.roleName, output);
            MarshallUtil.marshallString(obj.containerId, output);
        }

        @Override
        public RoleRemovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public RoleRemovedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            RoleRemovedEvent res = new RoleRemovedEvent();
            res.roleId = MarshallUtil.unmarshallString(input);
            res.roleName = MarshallUtil.unmarshallString(input);
            res.containerId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
