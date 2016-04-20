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

    @Override
    public void onEvent(Event event) {
        events.add(event);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // TODO: implement if needed
    }

    @Override
    public void close() {

    }

    public static BlockingQueue<Event> getInstance() {
        return events;
    }
}
