/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.config;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.events.EventType;

import org.junit.Test;

import static org.junit.Assert.fail;

public class EventOptionsTest {

    @Test
    public void testDeprecatedArePresent() {
        List<String> deprecatedValues = new ArrayList<>(EventOptions.USER_EVENT_METRICS_EVENTS.getDeprecatedMetadata().get().getDeprecatedValues());
        for (EventType event : EventType.values()) {
            String value = event.name().toLowerCase();
            deprecatedValues.remove(value);
        }
        if (!deprecatedValues.isEmpty()) {
            fail("Unknown event types " + deprecatedValues + " found in event-metrics-user-events");
        }
    }

}
