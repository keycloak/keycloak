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

package org.keycloak.testsuite.rest.representation;

import org.keycloak.events.user.UserEventType;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Objects;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class TestUserEvent {
    private String id;
    private UserEventType eventType;
    private UserRepresentation previousRepresentation;
    private UserRepresentation representation;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEventType getEventType() {
        return eventType;
    }

    public void setEventType(UserEventType eventType) {
        this.eventType = eventType;
    }

    public UserRepresentation getPreviousRepresentation() {
        return previousRepresentation;
    }

    public void setPreviousRepresentation(UserRepresentation previousRepresentation) {
        this.previousRepresentation = previousRepresentation;
    }

    public UserRepresentation getRepresentation() {
        return representation;
    }

    public void setRepresentation(UserRepresentation representation) {
        this.representation = representation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestUserEvent that = (TestUserEvent) o;
        return id.equals(that.id) &&
                eventType == that.eventType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
