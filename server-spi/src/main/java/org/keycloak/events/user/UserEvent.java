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

package org.keycloak.events.user;

import org.keycloak.representations.idm.UserRepresentation;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UserEvent {
    private final String id;
    private final long timeStamp;
    private final UserEventType eventType;
    private final UserRepresentation previousRepresentation;
    private final UserRepresentation representation;
    private final Map<String, Object> contextAttributes;

    private UserEvent(String id, long timeStamp, UserEventType eventType, UserRepresentation previousRepresentation, UserRepresentation representation, Map<String, Object> contextAttributes) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.eventType = eventType;
        this.previousRepresentation = previousRepresentation;
        this.representation = representation;
        this.contextAttributes = contextAttributes;
    }

    public String getId() {
        return id;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public UserEventType getEventType() {
        return eventType;
    }

    public UserRepresentation getPreviousRepresentation() {
        return previousRepresentation;
    }

    public UserRepresentation getRepresentation() {
        return representation;
    }

    public Map<String, Object> getContextAttributes() {
        return Collections.unmodifiableMap(contextAttributes);
    }

    public static Builder builder(String id, UserEventType eventType) {
        return new Builder(id, eventType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEvent event = (UserEvent) o;
        return getId().equals(event.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public static class Builder {
        private final String id;
        private long timeStamp;
        private final UserEventType eventType;
        private UserRepresentation previousRepresentation;
        private UserRepresentation representation;
        private final Map<String, Object> contextAttributes = new HashMap<>();

        public Builder(String id, UserEventType eventType) {
            this.id = id;
            this.timeStamp = Instant.now().getEpochSecond();
            this.eventType = eventType;
        }

        public Builder setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
            return this;
        }

        public Builder setPreviousRepresentation(UserRepresentation previousRepresentation) {
            this.previousRepresentation = previousRepresentation;
            return this;
        }

        public Builder setRepresentation(UserRepresentation representation) {
            this.representation = representation;
            return this;
        }

        public Builder addContextAttribute(String key, Object value) {
            contextAttributes.put(key, value);
            return this;
        }

        public UserEvent build() {
            return new UserEvent(id, timeStamp, eventType, previousRepresentation, representation, contextAttributes);
        }
    }
}
