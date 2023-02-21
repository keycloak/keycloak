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

package org.keycloak.models.sessions.infinispan.events;

import org.keycloak.models.KeycloakSession;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(ClientRemovedSessionEvent.ExternalizerImpl.class)
public class ClientRemovedSessionEvent extends SessionClusterEvent  {

    private String clientUuid;

    public static ClientRemovedSessionEvent create(KeycloakSession session, String eventKey, String realmId, boolean resendingEvent, String clientUuid) {
        ClientRemovedSessionEvent event = ClientRemovedSessionEvent.createEvent(ClientRemovedSessionEvent.class, eventKey, session, realmId, resendingEvent);
        event.clientUuid = clientUuid;
        return event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ClientRemovedSessionEvent that = (ClientRemovedSessionEvent) o;
        return Objects.equals(clientUuid, that.clientUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clientUuid);
    }

    @Override
    public String toString() {
        return String.format("ClientRemovedSessionEvent [ realmId=%s , clientUuid=%s ]", getRealmId(), clientUuid);
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public static class ExternalizerImpl implements Externalizer<ClientRemovedSessionEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, ClientRemovedSessionEvent obj) throws IOException {
            output.writeByte(VERSION_1);
            obj.marshallTo(output);
            MarshallUtil.marshallString(obj.clientUuid, output);
        }

        @Override
        public ClientRemovedSessionEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public ClientRemovedSessionEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            ClientRemovedSessionEvent res = new ClientRemovedSessionEvent();
            res.unmarshallFrom(input);
            res.clientUuid = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
