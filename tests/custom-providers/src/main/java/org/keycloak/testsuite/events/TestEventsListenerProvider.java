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
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class TestEventsListenerProvider implements EventListenerProvider {

    private static final BlockingQueue<Event> events = new LinkedBlockingQueue<Event>();
    private static final BlockingQueue<AdminEvent> adminEvents = new LinkedBlockingQueue<>();
    private final EventListenerTransaction tx = new EventListenerTransaction((event, includeRepre) -> adminEvents.add(event), events::add);

    public TestEventsListenerProvider(KeycloakSession session) {
        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void onEvent(Event event) {
        tx.addEvent(event);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        tx.addAdminEvent(event, includeRepresentation);
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
}
