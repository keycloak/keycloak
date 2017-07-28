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

import org.keycloak.cluster.ClusterEvent;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WrapperClusterEvent implements ClusterEvent {

    private String eventKey;
    private String sender;
    private String senderSite;
    private boolean ignoreSender;
    private boolean ignoreSenderSite;
    private ClusterEvent delegateEvent;

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderSite() {
        return senderSite;
    }

    public void setSenderSite(String senderSite) {
        this.senderSite = senderSite;
    }

    public boolean isIgnoreSender() {
        return ignoreSender;
    }

    public void setIgnoreSender(boolean ignoreSender) {
        this.ignoreSender = ignoreSender;
    }

    public boolean isIgnoreSenderSite() {
        return ignoreSenderSite;
    }

    public void setIgnoreSenderSite(boolean ignoreSenderSite) {
        this.ignoreSenderSite = ignoreSenderSite;
    }

    public ClusterEvent getDelegateEvent() {
        return delegateEvent;
    }

    public void setDelegateEvent(ClusterEvent delegateEvent) {
        this.delegateEvent = delegateEvent;
    }

    @Override
    public String toString() {
        return String.format("WrapperClusterEvent [ eventKey=%s, sender=%s, senderSite=%s, delegateEvent=%s ]", eventKey, sender, senderSite, delegateEvent.toString());
    }
}
