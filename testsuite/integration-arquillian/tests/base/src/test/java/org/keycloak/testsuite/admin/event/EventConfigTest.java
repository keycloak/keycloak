/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.admin.event;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test updates to the events configuration.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class EventConfigTest extends AbstractEventTest {

    @Test
    public void defaultEventConfigTest() {
        assertFalse(configRep.isAdminEventsDetailsEnabled());
        assertFalse(configRep.isAdminEventsEnabled());
        assertFalse(configRep.isEventsEnabled());

        List<String> eventListeners = configRep.getEventsListeners();
        assertEquals(1, eventListeners.size());
        assertEquals("jboss-logging", eventListeners.get(0));
    }

    @Test
    public void enableEventsTest() {
        enableEvents();

        assertTrue(configRep.isEventsEnabled());
        assertTrue(configRep.isAdminEventsEnabled());
    }

    @Test
    public void addRemoveListenerTest() {
        configRep.setEventsListeners(Collections.EMPTY_LIST);
        saveConfig();
        assertEquals(0, configRep.getEventsListeners().size());

        configRep.setEventsListeners(Arrays.asList("email"));
        saveConfig();
        List<String> eventListeners = configRep.getEventsListeners();
        assertEquals(1, eventListeners.size());
        assertEquals("email", eventListeners.get(0));
    }

    @Test
    public void loginEventSettingsTest() {
        enableEvents();

        assertTrue(hasEventType("LOGIN"));
        assertTrue(hasEventType("LOGOUT"));
        assertTrue(hasEventType("CLIENT_DELETE_ERROR"));

        int defaultEventCount = configRep.getEnabledEventTypes().size();

        configRep.setEnabledEventTypes(Arrays.asList("CLIENT_DELETE", "CLIENT_DELETE_ERROR"));
        saveConfig();

        List<String> enabledEventTypes = configRep.getEnabledEventTypes();
        assertEquals(2, enabledEventTypes.size());

        // remove all event types
        configRep.setEnabledEventTypes(Collections.EMPTY_LIST);
        saveConfig();

        // removing all event types restores default events
        assertEquals(defaultEventCount, configRep.getEnabledEventTypes().size());
    }

    private boolean hasEventType(String eventType) {
        for (String event : configRep.getEnabledEventTypes()) {
            if (eventType.equals(event)) return true;
        }

        return false;
    }

    @Test
    public void includeRepresentationTest() {
        enableEvents();

        assertTrue(configRep.isAdminEventsEnabled());
        assertFalse(configRep.isAdminEventsDetailsEnabled());

        configRep.setAdminEventsDetailsEnabled(Boolean.TRUE);
        saveConfig();

        assertTrue(configRep.isAdminEventsDetailsEnabled());
    }

    @Test
    public void setLoginEventExpirationTest() {
        enableEvents();

        assertNull(configRep.getEventsExpiration());

        Long oneHour = 3600L;
        configRep.setEventsExpiration(oneHour);
        saveConfig();

        assertEquals(oneHour, configRep.getEventsExpiration());
    }
}
