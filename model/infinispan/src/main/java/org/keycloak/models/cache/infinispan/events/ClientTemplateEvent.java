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
 * TODO Leave the name ClientTemplateEvent just due the backwards compatibility of infinispan migration. See if can be renamed based on
 * rolling upgrades plan...
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(ClientTemplateEvent.ExternalizerImpl.class)
public class ClientTemplateEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String clientTemplateId;

    public static ClientTemplateEvent create(String clientTemplateId) {
        ClientTemplateEvent event = new ClientTemplateEvent();
        event.clientTemplateId = clientTemplateId;
        return event;
    }

    @Override
    public String getId() {
        return clientTemplateId;
    }


    @Override
    public String toString() {
        return "ClientTemplateEvent [ " + clientTemplateId + " ]";
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        // Nothing. ID was already invalidated
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ClientTemplateEvent that = (ClientTemplateEvent) o;
        return Objects.equals(clientTemplateId, that.clientTemplateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clientTemplateId);
    }

    public static class ExternalizerImpl implements Externalizer<ClientTemplateEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, ClientTemplateEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.clientTemplateId, output);
        }

        @Override
        public ClientTemplateEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public ClientTemplateEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            ClientTemplateEvent res = new ClientTemplateEvent();
            res.clientTemplateId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
