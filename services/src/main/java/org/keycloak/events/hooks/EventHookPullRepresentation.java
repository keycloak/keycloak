/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events.hooks;

import java.util.List;

public class EventHookPullRepresentation {

    private Object event;
    private List<Object> events;
    private EventHookPullEntryRepresentation entry;
    private List<EventHookPullEntryRepresentation> entries;
    private boolean hasMoreEvents;

    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
        this.event = event;
    }

    public List<Object> getEvents() {
        return events;
    }

    public void setEvents(List<Object> events) {
        this.events = events;
    }

    public EventHookPullEntryRepresentation getEntry() {
        return entry;
    }

    public void setEntry(EventHookPullEntryRepresentation entry) {
        this.entry = entry;
    }

    public List<EventHookPullEntryRepresentation> getEntries() {
        return entries;
    }

    public void setEntries(List<EventHookPullEntryRepresentation> entries) {
        this.entries = entries;
    }

    public boolean isHasMoreEvents() {
        return hasMoreEvents;
    }

    public void setHasMoreEvents(boolean hasMoreEvents) {
        this.hasMoreEvents = hasMoreEvents;
    }
}
