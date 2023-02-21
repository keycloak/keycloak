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
@SerializeWith(RoleAddedEvent.ExternalizerImpl.class)
public class RoleAddedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String roleId;
    private String containerId;

    public static RoleAddedEvent create(String roleId, String containerId) {
        RoleAddedEvent event = new RoleAddedEvent();
        event.roleId = roleId;
        event.containerId = containerId;
        return event;
    }

    @Override
    public String getId() {
        return roleId;
    }

    @Override
    public String toString() {
        return String.format("RoleAddedEvent [ roleId=%s, containerId=%s ]", roleId, containerId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.roleAdded(containerId, invalidations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RoleAddedEvent that = (RoleAddedEvent) o;
        return Objects.equals(roleId, that.roleId) && Objects.equals(containerId, that.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), roleId, containerId);
    }

    public static class ExternalizerImpl implements Externalizer<RoleAddedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, RoleAddedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.roleId, output);
            MarshallUtil.marshallString(obj.containerId, output);
        }

        @Override
        public RoleAddedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public RoleAddedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            RoleAddedEvent res = new RoleAddedEvent();
            res.roleId = MarshallUtil.unmarshallString(input);
            res.containerId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
