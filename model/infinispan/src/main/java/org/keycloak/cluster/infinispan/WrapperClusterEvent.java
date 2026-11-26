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

package org.keycloak.cluster.infinispan;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.WRAPPED_CLUSTER_EVENT)
@ProtoReserved(
        value = {3, 4},
        names = {"senderSite", "siteFilter"}
)
public class WrapperClusterEvent implements ClusterEvent {

    @ProtoField(1)
    final String eventKey;
    @ProtoField(2)
    final String senderAddress; // null means invoke everywhere
    private final Collection<? extends ClusterEvent> events;

    private WrapperClusterEvent(String eventKey, String senderAddress, Collection<? extends ClusterEvent> events) {
        this.eventKey = Objects.requireNonNull(eventKey);
        this.senderAddress = senderAddress;
        this.events = Objects.requireNonNull(events);
    }

    @ProtoFactory
    static WrapperClusterEvent protoFactory(String eventKey, String senderAddress, List<WrappedMessage> eventPS) {
        var events = eventPS.stream().map(WrappedMessage::getValue).map(ClusterEvent.class::cast).toList();
        return new WrapperClusterEvent(eventKey, senderAddress, events);
    }

    public static WrapperClusterEvent wrap(String eventKey, Collection<? extends ClusterEvent> events, String senderAddress, boolean ignoreSender) {
        senderAddress = ignoreSender ? Objects.requireNonNull(senderAddress) : null;
        return new WrapperClusterEvent(eventKey, senderAddress, events);
    }

    @ProtoField(5)
    List<WrappedMessage> getEventPS() {
        return events.stream().map(WrappedMessage::new).toList();
    }

    public String getEventKey() {
        return eventKey;
    }

    public Collection<? extends ClusterEvent> getDelegateEvents() {
        return events;
    }

    public boolean rejectEvent(String mySiteAddress) {
        return senderAddress != null && senderAddress.equals(mySiteAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WrapperClusterEvent that = (WrapperClusterEvent) o;
        return eventKey.equals(that.eventKey) &&
                Objects.equals(senderAddress, that.senderAddress) &&
                events.equals(that.events);
    }

    @Override
    public int hashCode() {
        int result = eventKey.hashCode();
        result = 31 * result + Objects.hashCode(senderAddress);
        result = 31 * result + events.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("WrapperClusterEvent [ eventKey=%s, sender=%s, delegateEvents=%s ]", eventKey, senderAddress, events);
    }
}
