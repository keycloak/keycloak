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
@SerializeWith(ClientAddedEvent.ExternalizerImpl.class)
public class ClientAddedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String clientUuid;
    private String clientId;
    private String realmId;

    public static ClientAddedEvent create(String clientUuid, String clientId, String realmId) {
        ClientAddedEvent event = new ClientAddedEvent();
        event.clientUuid = clientUuid;
        event.clientId = clientId;
        event.realmId = realmId;
        return event;
    }

    @Override
    public String getId() {
        return clientUuid;
    }

    @Override
    public String toString() {
        return String.format("ClientAddedEvent [ realmId=%s, clientUuid=%s, clientId=%s ]", realmId, clientUuid, clientId);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.clientAdded(realmId, clientUuid, clientId, invalidations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ClientAddedEvent that = (ClientAddedEvent) o;
        return Objects.equals(clientUuid, that.clientUuid) && Objects.equals(clientId, that.clientId) && Objects.equals(realmId, that.realmId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clientUuid, clientId, realmId);
    }

    public static class ExternalizerImpl implements Externalizer<ClientAddedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, ClientAddedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.clientUuid, output);
            MarshallUtil.marshallString(obj.clientId, output);
            MarshallUtil.marshallString(obj.realmId, output);
        }

        @Override
        public ClientAddedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public ClientAddedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            ClientAddedEvent res = new ClientAddedEvent();
            res.clientUuid = MarshallUtil.unmarshallString(input);
            res.clientId = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
