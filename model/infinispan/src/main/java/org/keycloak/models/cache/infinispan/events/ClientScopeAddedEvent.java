/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

@SerializeWith(ClientScopeAddedEvent.ExternalizerImpl.class)
public class ClientScopeAddedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String clientScopeId;
    private String realmId;

    public static ClientScopeAddedEvent create(String clientScopeId, String realmId) {
        ClientScopeAddedEvent event = new ClientScopeAddedEvent();
        event.clientScopeId = clientScopeId;
        event.realmId = realmId;
        return event;
    }

    @Override
    public String getId() {
        return clientScopeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ClientScopeAddedEvent that = (ClientScopeAddedEvent) o;
        return Objects.equals(clientScopeId, that.clientScopeId) && Objects.equals(realmId, that.realmId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clientScopeId, realmId);
    }

    @Override
    public String toString() {
        return String.format("ClientScopeAddedEvent [ clientScopeId=%s, realmId=%s ]", clientScopeId, realmId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.clientScopeAdded(realmId, invalidations);
    }

    public static class ExternalizerImpl implements Externalizer<ClientScopeAddedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, ClientScopeAddedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.clientScopeId, output);
            MarshallUtil.marshallString(obj.realmId, output);
        }

        @Override
        public ClientScopeAddedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public ClientScopeAddedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            ClientScopeAddedEvent res = new ClientScopeAddedEvent();
            res.clientScopeId = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
