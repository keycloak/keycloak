/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.authSession;

import org.keycloak.cluster.ClusterEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapAuthenticationSessionAuthNoteUpdateEvent implements ClusterEvent {

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
    public static MapAuthenticationSessionAuthNoteUpdateEvent create(String authSessionId, String tabId, String clientUUID,
                                                                     Map<String, String> authNotesFragment) {
        MapAuthenticationSessionAuthNoteUpdateEvent event = new MapAuthenticationSessionAuthNoteUpdateEvent();
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapAuthenticationSessionAuthNoteUpdateEvent that = (MapAuthenticationSessionAuthNoteUpdateEvent) o;
        return Objects.equals(authSessionId, that.authSessionId) && Objects.equals(tabId, that.tabId) && Objects.equals(clientUUID, that.clientUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authSessionId, tabId, clientUUID);
    }

    @Override
    public String toString() {
        return String.format("AuthenticationSessionAuthNoteUpdateEvent [ authSessionId=%s, tabId=%s, clientUUID=%s, authNotesFragment=%s ]",
                authSessionId, tabId, clientUUID, authNotesFragment);
    }
}
