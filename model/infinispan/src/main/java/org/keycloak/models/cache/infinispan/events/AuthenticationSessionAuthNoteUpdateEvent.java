/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.cluster.ClusterEvent;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 *
 * @author hmlnarik
 */
@SerializeWith(AuthenticationSessionAuthNoteUpdateEvent.ExternalizerImpl.class)
public class AuthenticationSessionAuthNoteUpdateEvent implements ClusterEvent {

    private String authSessionId;
    private String tabId;
    private String clientUUID;

    private Map<String, String> authNotesFragment;

    /**
     * Creates an instance of the event.
     * @param authSessionId
     * @param authNotesFragment
     * @return Event. Note that {@code authNotesFragment} property is not thread safe which is fine for now.
     */
    public static AuthenticationSessionAuthNoteUpdateEvent create(String authSessionId, String tabId, String clientUUID, Map<String, String> authNotesFragment) {
        AuthenticationSessionAuthNoteUpdateEvent event = new AuthenticationSessionAuthNoteUpdateEvent();
        event.authSessionId = authSessionId;
        event.tabId = tabId;
        event.clientUUID = clientUUID;
        event.authNotesFragment = new LinkedHashMap<>(authNotesFragment);
        return event;
    }

    public String getAuthSessionId() {
        return authSessionId;
    }

    public String getTabId() {
        return tabId;
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public Map<String, String> getAuthNotesFragment() {
        return authNotesFragment;
    }

    @Override
    public String toString() {
        return String.format("AuthenticationSessionAuthNoteUpdateEvent [ authSessionId=%s, tabId=%s, clientUUID=%s, authNotesFragment=%s ]",
                authSessionId, clientUUID, authNotesFragment);
    }

    public static class ExternalizerImpl implements Externalizer<AuthenticationSessionAuthNoteUpdateEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, AuthenticationSessionAuthNoteUpdateEvent value) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(value.authSessionId, output);
            MarshallUtil.marshallString(value.tabId, output);
            MarshallUtil.marshallString(value.clientUUID, output);
            MarshallUtil.marshallMap(value.authNotesFragment, output);
        }

        @Override
        public AuthenticationSessionAuthNoteUpdateEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public AuthenticationSessionAuthNoteUpdateEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            return create(
              MarshallUtil.unmarshallString(input),
              MarshallUtil.unmarshallString(input),
              MarshallUtil.unmarshallString(input),
              MarshallUtil.unmarshallMap(input, HashMap::new)
            );
        }

    }
}
