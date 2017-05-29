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

import java.util.List;
import java.util.Map;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;

/**
 * Postpone sending notifications of session events to the commit of Keycloak transaction
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionEventsSenderTransaction extends AbstractKeycloakTransaction {

    private final KeycloakSession session;

    private final MultivaluedHashMap<String, SessionClusterEvent> sessionEvents = new MultivaluedHashMap<>();
    private final MultivaluedHashMap<String, SessionClusterEvent> localDCSessionEvents = new MultivaluedHashMap<>();

    public SessionEventsSenderTransaction(KeycloakSession session) {
        this.session = session;
    }

    public void addEvent(String eventName, SessionClusterEvent event, boolean sendToAllDCs) {
        if (sendToAllDCs) {
            sessionEvents.add(eventName, event);
        } else {
            localDCSessionEvents.add(eventName, event);
        }
    }

    @Override
    protected void commitImpl() {
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        // TODO bulk notify (send whole list instead of separate events?)
        for (Map.Entry<String, List<SessionClusterEvent>> entry : sessionEvents.entrySet()) {
            for (SessionClusterEvent event : entry.getValue()) {
                cluster.notify(entry.getKey(), event, false, ClusterProvider.DCNotify.ALL_DCS);
            }
        }

        for (Map.Entry<String, List<SessionClusterEvent>> entry : localDCSessionEvents.entrySet()) {
            for (SessionClusterEvent event : entry.getValue()) {
                cluster.notify(entry.getKey(), event, false, ClusterProvider.DCNotify.LOCAL_DC_ONLY);
            }
        }
    }

    @Override
    protected void rollbackImpl() {

    }
}
