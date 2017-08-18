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

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRemovedSessionEvent extends SessionClusterEvent  {

    private String clientUuid;

    public static ClientRemovedSessionEvent create(KeycloakSession session, String eventKey, String realmId, boolean resendingEvent, String clientUuid) {
        ClientRemovedSessionEvent event = ClientRemovedSessionEvent.createEvent(ClientRemovedSessionEvent.class, eventKey, session, realmId, resendingEvent);
        event.clientUuid = clientUuid;
        return event;
    }

    @Override
    public String toString() {
        return String.format("ClientRemovedSessionEvent [ realmId=%s , clientUuid=%s ]", getRealmId(), clientUuid);
    }

    public String getClientUuid() {
        return clientUuid;
    }
}
