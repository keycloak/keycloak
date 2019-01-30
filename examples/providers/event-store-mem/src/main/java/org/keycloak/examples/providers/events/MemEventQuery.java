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

package org.keycloak.examples.providers.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventType;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemEventQuery implements EventQuery {

    private List<Event> events;

    private int first;
    private int max;

    public MemEventQuery(List<Event> events) {
        this.events = events;
    }

    @Override
    public EventQuery type(EventType... types) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            Event next = itr.next();
            boolean include = false;
            for (EventType e : types) {
                if (next.getType().equals(e)) {
                    include = true;
                    break;
                }
            }
            if (!include) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery realm(String realmId) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getRealmId().equals(realmId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery client(String clientId) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getClientId().equals(clientId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery user(String userId) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getUserId().equals(userId)) {
                itr.remove();
            }
        }
        return this;
    }
    
    @Override
    public EventQuery fromDate(Date fromDate) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!(itr.next().getTime() >= fromDate.getTime())) {
                itr.remove();
            }
        }
        return this;
    }
    
    @Override
    public EventQuery toDate(Date toDate) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!(itr.next().getTime() <= toDate.getTime())) {
                itr.remove();
            }
        }
        return this;
    }
    
    @Override
    public EventQuery ipAddress(String ipAddress) {
        Iterator<Event> itr = this.events.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getIpAddress().equals(ipAddress)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public EventQuery firstResult(int result) {
        this.first = result;
        return this;
    }

    @Override
    public EventQuery maxResults(int results) {
        this.max = results;
        return this;
    }

    @Override
    public List<Event> getResultList() {
        if (events.size() < first) {
            return Collections.emptyList();
        }
        int end = first + max <= events.size() ? first + max : events.size();

        return events.subList(first, end);
    }

}
