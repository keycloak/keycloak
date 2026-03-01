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
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.WRAPPED_CLUSTER_EVENT)
public class WrapperClusterEvent implements ClusterEvent {

    @ProtoField(1)
    final String eventKey;
    @ProtoField(2)
    final String senderAddress; // null means invoke everywhere
    @ProtoField(3)
    final String senderSite; // can be null
    @ProtoField(4)
    final SiteFilter siteFilter;
    private final Collection<? extends ClusterEvent> events;

    private WrapperClusterEvent(String eventKey, String senderAddress, String senderSite, SiteFilter siteFilter, Collection<? extends ClusterEvent> events) {
        this.eventKey = Objects.requireNonNull(eventKey);
        this.senderAddress = senderAddress;
        this.senderSite = senderSite;
        this.siteFilter = Objects.requireNonNull(siteFilter);
        this.events = Objects.requireNonNull(events);
    }

    @ProtoFactory
    static WrapperClusterEvent protoFactory(String eventKey, String senderAddress, String senderSite, SiteFilter siteFilter, List<WrappedMessage> eventPS) {
        var events = eventPS.stream().map(WrappedMessage::getValue).map(ClusterEvent.class::cast).toList();
        return new WrapperClusterEvent(eventKey, senderAddress, senderSite, siteFilter, events);
    }

    public static WrapperClusterEvent wrap(String eventKey, Collection<? extends ClusterEvent> events, String senderAddress, String senderSite, ClusterProvider.DCNotify dcNotify, boolean ignoreSender) {
        senderAddress = ignoreSender ? Objects.requireNonNull(senderAddress) : null;
        senderSite = dcNotify == ClusterProvider.DCNotify.ALL_DCS ? null : senderSite;
        var siteNotification = switch (dcNotify) {
            case ALL_DCS -> SiteFilter.ALL;
            case LOCAL_DC_ONLY -> SiteFilter.LOCAL;
            case ALL_BUT_LOCAL_DC -> SiteFilter.REMOTE;
        };
        return new WrapperClusterEvent(eventKey, senderAddress, senderSite, siteNotification, events);
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

    public boolean rejectEvent(String mySiteAddress, String mySiteName) {
        return (senderAddress != null && senderAddress.equals(mySiteAddress)) ||
                (senderSite != null  && siteFilter.reject(senderSite, mySiteName));

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WrapperClusterEvent that = (WrapperClusterEvent) o;
        return eventKey.equals(that.eventKey) &&
                Objects.equals(senderAddress, that.senderAddress) &&
                Objects.equals(senderSite, that.senderSite) &&
                siteFilter == that.siteFilter &&
                events.equals(that.events);
    }

    @Override
    public int hashCode() {
        int result = eventKey.hashCode();
        result = 31 * result + Objects.hashCode(senderAddress);
        result = 31 * result + Objects.hashCode(senderSite);
        result = 31 * result + siteFilter.hashCode();
        result = 31 * result + events.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("WrapperClusterEvent [ eventKey=%s, sender=%s, senderSite=%s, delegateEvents=%s ]", eventKey, senderAddress, senderSite, events);
    }

    @Proto
    @ProtoTypeId(Marshalling.WRAPPED_CLUSTER_EVENT_SITE_FILTER)
    public enum SiteFilter {
        ALL {
            @Override
            boolean reject(String senderSite, String mySite) {
                return false;
            }
        }, LOCAL {
            @Override
            boolean reject(String senderSite, String mySite) {
                return !Objects.equals(senderSite, mySite);
            }
        }, REMOTE {
            @Override
            boolean reject(String senderSite, String mySite) {
                return Objects.equals(senderSite, mySite);
            }
        };

        abstract boolean reject(String senderSite, String mySite);
    }
}
