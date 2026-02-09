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

package org.keycloak.tests.admin.event;

import java.util.List;

import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test updates to the events configuration.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class EventConfigTest {

    @InjectRealm(config = EventConfigRealmConfig.class)
    ManagedRealm configRealm;

    RealmEventsConfigRepresentation configRep;

    @BeforeEach
    public void setConfigRep() {
        configRep = configRealm.admin().getRealmEventsConfig();
    }

    @Test
    public void defaultEventConfigTest() {
        configRealm.updateWithCleanup(r -> r.eventsEnabled(false).adminEventsEnabled(false));

        configRep = configRealm.admin().getRealmEventsConfig();
        Assertions.assertFalse(configRep.isAdminEventsDetailsEnabled());
        Assertions.assertFalse(configRep.isAdminEventsEnabled());
        Assertions.assertFalse(configRep.isEventsEnabled());

        List<String> eventListeners = configRep.getEventsListeners();
        Assertions.assertEquals(1, eventListeners.size());
        Assertions.assertEquals("jboss-logging", eventListeners.get(0));
    }

    @Test
    public void enableEventsTest() {
        Assertions.assertTrue(configRep.isEventsEnabled());
        Assertions.assertTrue(configRep.isAdminEventsEnabled());
    }

    @Test
    public void addRemoveListenerTest() {
        configRealm.updateWithCleanup(r -> r.eventsEnabled(false).adminEventsEnabled(false).overwriteEventsListeners());

        configRep = configRealm.admin().getRealmEventsConfig();

        Assertions.assertEquals(0, configRep.getEventsListeners().size());

        configRealm.updateWithCleanup(r -> r.overwriteEventsListeners("email"));
        configRep = configRealm.admin().getRealmEventsConfig();
        List<String> eventListeners = configRep.getEventsListeners();
        Assertions.assertEquals(1, eventListeners.size());
        Assertions.assertEquals("email", eventListeners.get(0));
    }

    @Test
    public void loginEventSettingsTest() {
        Assertions.assertTrue(hasEventType("LOGIN"));
        Assertions.assertTrue(hasEventType("LOGOUT"));
        Assertions.assertTrue(hasEventType("CLIENT_DELETE_ERROR"));

        int defaultEventCount = configRep.getEnabledEventTypes().size();

        configRealm.updateWithCleanup(r -> r.enabledEventTypes("CLIENT_DELETE", "CLIENT_DELETE_ERROR"));

        List<String> enabledEventTypes = configRealm.admin().getRealmEventsConfig().getEnabledEventTypes();
        Assertions.assertEquals(2, enabledEventTypes.size());

        // remove all event types
        configRealm.updateWithCleanup(RealmConfigBuilder::setEnabledEventTypes);

        // removing all event types restores default events
        Assertions.assertEquals(defaultEventCount, configRealm.admin().getRealmEventsConfig().getEnabledEventTypes().size());
    }

    private boolean hasEventType(String eventType) {
        for (String event : configRep.getEnabledEventTypes()) {
            if (eventType.equals(event)) {
                return true;
            }
        }

        return false;
    }

    @Test
    public void includeRepresentationTest() {
        Assertions.assertTrue(configRep.isAdminEventsEnabled());
        Assertions.assertFalse(configRep.isAdminEventsDetailsEnabled());

        configRealm.updateWithCleanup(r -> r.adminEventsDetailsEnabled(true));

        Assertions.assertTrue(configRealm.admin().getRealmEventsConfig().isAdminEventsDetailsEnabled());
    }

    @Test
    public void setLoginEventExpirationTest() {
        Assertions.assertNull(configRep.getEventsExpiration());

        long oneHour = 3600;
        configRealm.updateWithCleanup(r -> r.eventsExpiration(oneHour));

        Assertions.assertEquals(oneHour, configRealm.admin().getRealmEventsConfig().getEventsExpiration());
    }

    public static class EventConfigRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.eventsEnabled(true).adminEventsEnabled(true);
        }
    }
}
