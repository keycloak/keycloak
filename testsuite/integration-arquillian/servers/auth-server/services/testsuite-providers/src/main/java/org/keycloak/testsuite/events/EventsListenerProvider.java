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

package org.keycloak.testsuite.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class EventsListenerProvider implements EventListenerProvider {

    private static final BlockingQueue<Event> events = new LinkedBlockingQueue<Event>();
    private static final BlockingQueue<AdminEvent> adminEvents = new LinkedBlockingQueue<>();

    @Override
    public void onEvent(Event event) {
        events.add(event);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Save the copy for case when same AdminEventBuilder is used more times during same transaction to avoid overwriting previously referenced event
        adminEvents.add(copy(event));
    }

    @Override
    public void close() {

    }

    public static Event poll() {
        return events.poll();
    }

    public static AdminEvent pollAdminEvent() {
        return adminEvents.poll();
    }

    public static void clear() {
        events.clear();
    }

    public static void clearAdminEvents() {
        adminEvents.clear();
    }

    private AdminEvent copy(AdminEvent adminEvent) {
        AdminEvent newEvent = new AdminEvent();
        newEvent.setAuthDetails(adminEvent.getAuthDetails());
        newEvent.setError(adminEvent.getError());
        newEvent.setOperationType(adminEvent.getOperationType());
        newEvent.setResourceType(adminEvent.getResourceType());
        newEvent.setRealmId(adminEvent.getRealmId());
        newEvent.setRepresentation(adminEvent.getRepresentation());
        newEvent.setResourcePath(adminEvent.getResourcePath());
        newEvent.setTime(adminEvent.getTime());
        return newEvent;
    }
}
