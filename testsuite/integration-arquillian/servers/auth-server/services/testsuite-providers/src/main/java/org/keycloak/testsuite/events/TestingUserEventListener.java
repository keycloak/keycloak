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

package org.keycloak.testsuite.events;

import org.keycloak.events.user.UserEvent;
import org.keycloak.events.user.UserEventListener;
import org.keycloak.testsuite.rest.representation.TestUserEvent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class TestingUserEventListener implements UserEventListener {
    private static Queue<UserEvent> preEvents = new LinkedList<>();
    private static Queue<UserEvent> postEvents = new LinkedList<>();

    @Override
    public ApprovalResult handlePreEvent(UserEvent event) {
        preEvents.add(event);
        return ApprovalResult.APPROVED;
    }

    @Override
    public void handlePostEvent(UserEvent event) {
        postEvents.add(event);
    }

    public TestUserEvent pollPreEvent() {
        return toRep(preEvents.poll());
    }

    public TestUserEvent pollPostEvent() {
        return toRep(postEvents.poll());
    }

    public int getPreEventsCount() {
        return preEvents.size();
    }

    public int getPostEventsCount() {
        return postEvents.size();
    }

    public void clearEvents() {
        preEvents.clear();
        postEvents.clear();
    }

    private TestUserEvent toRep(UserEvent event) {
        if (event == null) return null;
        TestUserEvent rep = new TestUserEvent();
        rep.setId(event.getId());
        rep.setEventType(event.getEventType());
        rep.setPreviousRepresentation(event.getPreviousRepresentation());
        rep.setRepresentation(event.getRepresentation());
        return rep;
    }
}
