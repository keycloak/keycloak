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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 *
 * @author hmlnarik
 */
@ProtoTypeId(Marshalling.AUTHENTICATION_SESSION_AUTH_NOTE_UPDATE_EVENT)
public class AuthenticationSessionAuthNoteUpdateEvent implements ClusterEvent {

    private final String authSessionId;
    private final String tabId;
    private final Map<String, String> authNotesFragment;

    private AuthenticationSessionAuthNoteUpdateEvent(Map<String, String> authNotesFragment, String authSessionId, String tabId) {
        this.authNotesFragment = Objects.requireNonNull(authNotesFragment);
        this.authSessionId = Objects.requireNonNull(authSessionId);
        this.tabId = Objects.requireNonNull(tabId);
    }

    /**
     * Creates an instance of the event.
     *
     * @return Event. Note that {@code authNotesFragment} property is not thread safe which is fine for now.
     */
    @ProtoFactory
    public static AuthenticationSessionAuthNoteUpdateEvent create(String authSessionId, String tabId, Map<String, String> authNotesFragment) {
        return new AuthenticationSessionAuthNoteUpdateEvent(authNotesFragment, authSessionId, tabId);
    }

    @ProtoField(1)
    public String getAuthSessionId() {
        return authSessionId;
    }

    @ProtoField(2)
    public String getTabId() {
        return tabId;
    }

    @ProtoField(value = 3, mapImplementation = LinkedHashMap.class)
    public Map<String, String> getAuthNotesFragment() {
        return authNotesFragment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AuthenticationSessionAuthNoteUpdateEvent that = (AuthenticationSessionAuthNoteUpdateEvent) o;
        return Objects.equals(authSessionId, that.authSessionId) && Objects.equals(tabId, that.tabId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authSessionId, tabId);
    }

    @Override
    public String toString() {
        return String.format("AuthenticationSessionAuthNoteUpdateEvent [ authSessionId=%s, tabId=%s, authNotesFragment=%s ]",
                authSessionId, tabId, authNotesFragment);
    }

}
