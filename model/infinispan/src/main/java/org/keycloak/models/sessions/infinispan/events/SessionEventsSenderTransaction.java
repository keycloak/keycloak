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

import java.util.LinkedList;
import java.util.List;

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

    private final List<DCEventContext> sessionEvents = new LinkedList<>();

    public SessionEventsSenderTransaction(KeycloakSession session) {
        this.session = session;
    }

    public void addEvent(SessionClusterEvent event, ClusterProvider.DCNotify dcNotify) {
        sessionEvents.add(new DCEventContext(dcNotify, event));
    }


    @Override
    protected void commitImpl() {
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        // TODO bulk notify (send whole list instead of separate events?)
        for (DCEventContext entry : sessionEvents) {
            cluster.notify(entry.event.getEventKey(), entry.event, false, entry.dcNotify);
        }
    }


    @Override
    protected void rollbackImpl() {

    }


    private class DCEventContext {
        private final ClusterProvider.DCNotify dcNotify;
        private final SessionClusterEvent event;

        DCEventContext(ClusterProvider.DCNotify dcNotify, SessionClusterEvent event) {
            this.dcNotify = dcNotify;
            this.event = event;
        }
    }
}
