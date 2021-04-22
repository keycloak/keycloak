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
import org.keycloak.connections.infinispan.TopologyInfo;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import org.infinispan.commons.marshall.MarshallUtil;

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
        TopologyInfo topology = InfinispanUtil.getTopologyInfo(session);
        this.siteId = topology.getMySiteName();
        this.nodeId = topology.getMyNodeName();
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionClusterEvent that = (SessionClusterEvent) o;
        return Objects.equals(realmId, that.realmId) && Objects.equals(eventKey, that.eventKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realmId, eventKey);
    }

    @Override
    public String toString() {
        String simpleClassName = getClass().getSimpleName();
        return String.format("%s [ realmId=%s ]", simpleClassName, realmId);
    }

    // Infinispan marshalling support for child classes
    private static final int VERSION_1 = 1;

    protected void marshallTo(ObjectOutput output) throws IOException {
        output.writeByte(VERSION_1);

        MarshallUtil.marshallString(realmId, output);
        MarshallUtil.marshallString(eventKey, output);
        output.writeBoolean(resendingEvent);
        MarshallUtil.marshallString(siteId, output);
        MarshallUtil.marshallString(nodeId, output);
    }

    /**
     * Sets the properties of this object from the input stream.
     * @param input
     * @throws IOException
     */
    protected void unmarshallFrom(ObjectInput input) throws IOException {
        switch (input.readByte()) {
            case VERSION_1:
                unmarshallFromVersion1(input);
                break;
            default:
                throw new IOException("Unknown version");
        }
    }

    private void unmarshallFromVersion1(ObjectInput input) throws IOException {
        this.realmId = MarshallUtil.unmarshallString(input);
        this.eventKey = MarshallUtil.unmarshallString(input);
        this.resendingEvent = input.readBoolean();
        this.siteId = MarshallUtil.unmarshallString(input);
        this.nodeId = MarshallUtil.unmarshallString(input);
    }
}
