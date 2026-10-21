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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;

/**
 * Postpone sending notifications of session events to the commit of Keycloak transaction
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionEventsSenderTransaction extends AbstractKeycloakTransaction {

    private final KeycloakSession session;

    private final Map<String, List<ClusterEvent>> sessionEvents = new HashMap<>();

    public SessionEventsSenderTransaction(KeycloakSession session) {
        this.session = session;
    }

    public void addEvent(SessionClusterEvent event) {
        sessionEvents.computeIfAbsent(event.getEventKey(), eventGroup -> new ArrayList<>()).add(event);
    }

    @Override
    protected void commitImpl() {
        var cluster = session.getProvider(ClusterProvider.class);
        for (var entry : sessionEvents.entrySet()) {
            cluster.notify(entry.getKey(), entry.getValue(), false);
        }
    }


    @Override
    protected void rollbackImpl() {
        sessionEvents.clear();
    }

}
