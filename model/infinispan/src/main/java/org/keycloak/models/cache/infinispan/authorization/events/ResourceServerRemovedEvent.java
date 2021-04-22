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

package org.keycloak.models.cache.infinispan.authorization.events;

import org.keycloak.models.cache.infinispan.authorization.StoreFactoryCacheManager;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.Set;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(ResourceServerRemovedEvent.ExternalizerImpl.class)
public class ResourceServerRemovedEvent extends InvalidationEvent implements AuthorizationCacheInvalidationEvent {

    private String id;
    private String clientId;

    public static ResourceServerRemovedEvent create(String id, String clientId) {
        ResourceServerRemovedEvent event = new ResourceServerRemovedEvent();
        event.id = id;
        event.clientId = clientId;
        return event;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ResourceServerRemovedEvent that = (ResourceServerRemovedEvent) o;
        return Objects.equals(id, that.id) && Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, clientId);
    }

    @Override
    public String toString() {
        return String.format("ResourceServerRemovedEvent [ id=%s, clientId=%s ]", id, clientId);
    }

    @Override
    public void addInvalidations(StoreFactoryCacheManager cache, Set<String> invalidations) {
        cache.resourceServerRemoval(id, invalidations);
    }

    public static class ExternalizerImpl implements Externalizer<ResourceServerRemovedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, ResourceServerRemovedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.id, output);
            MarshallUtil.marshallString(obj.clientId, output);
        }

        @Override
        public ResourceServerRemovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public ResourceServerRemovedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            ResourceServerRemovedEvent res = new ResourceServerRemovedEvent();
            res.id = MarshallUtil.unmarshallString(input);
            res.clientId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
