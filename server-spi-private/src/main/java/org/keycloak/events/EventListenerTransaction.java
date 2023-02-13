/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.AbstractKeycloakTransaction;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EventListenerTransaction extends AbstractKeycloakTransaction {

    private static class AdminEventEntry {
        private final AdminEvent event;
        private final boolean includeRepresentation;

        public AdminEventEntry(AdminEvent event, boolean includeRepresentation) {
            this.event = event;
            this.includeRepresentation = includeRepresentation;
        }
    }

    private final List<AdminEventEntry> adminEventsToSend = new LinkedList<>();
    private final List<Event> eventsToSend = new LinkedList<>();
    private final BiConsumer<AdminEvent, Boolean> adminEventConsumer;
    private final Consumer<Event> eventConsumer;

    public EventListenerTransaction(BiConsumer<AdminEvent, Boolean> adminEventConsumer, Consumer<Event> eventConsumer) {
        this.adminEventConsumer = adminEventConsumer;
        this.eventConsumer = eventConsumer;
    }

    public void addAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        adminEventsToSend.add(new AdminEventEntry(adminEvent, includeRepresentation));
    }

    public void addEvent(Event event) {
        eventsToSend.add(event);
    }

    @Override
    protected void commitImpl() {
        adminEventsToSend.forEach(this::consumeAdminEventEntry);
        if (eventConsumer != null) {
            eventsToSend.forEach(eventConsumer);
        }
    }
    
    private void consumeAdminEventEntry(AdminEventEntry entry) {
        if (adminEventConsumer != null) {
            adminEventConsumer.accept(entry.event, entry.includeRepresentation);
        }
    }

    @Override
    protected void rollbackImpl() {
        adminEventsToSend.clear();
        eventsToSend.clear();
    }
    
    
}
