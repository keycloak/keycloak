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

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class SessionClusterEvent implements ClusterEvent {

    private String realmId;
    private String eventKey;
    private boolean resendingEvent;
    private String siteId;
    private String nodeId;


    public static <T extends SessionClusterEvent> T createEvent(Class<T> eventClass, String eventKey, KeycloakSession session, String realmId, boolean resendingEvent) {
        try {
            T event = eventClass.newInstance();
            event.setData(session, eventKey, realmId, resendingEvent);
            return event;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    void setData(KeycloakSession session, String eventKey, String realmId, boolean resendingEvent) {
        this.realmId = realmId;
        this.eventKey = eventKey;
        this.resendingEvent = resendingEvent;
        this.siteId = InfinispanUtil.getMySite(session);
        this.nodeId = InfinispanUtil.getMyAddress(session);
    }


    public String getRealmId() {
        return realmId;
    }

    public String getEventKey() {
        return eventKey;
    }

    public boolean isResendingEvent() {
        return resendingEvent;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        String simpleClassName = getClass().getSimpleName();
        return String.format("%s [ realmId=%s ]", simpleClassName, realmId);
    }
}
