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

package org.keycloak.testsuite.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.log.JBossLoggingEventListenerProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.events.EventsListenerProviderFactory;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.util.JsonSerialization;

import static org.junit.Assert.assertArrayEquals;

/**
 * This class adapts the functionality from the old testsuite to make tests
 * easier to port.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public abstract class AbstractAdminTest extends TestRealmKeycloakTest  {
    protected static final String REALM_NAME = "admin-client-test";

    protected RealmResource realm;
    protected String realmId;

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        findTestApp(testRealm).setDirectAccessGrantsEnabled(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);

        RealmRepresentation adminRealmRep = new RealmRepresentation();
        adminRealmRep.setRealm(REALM_NAME);
        adminRealmRep.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put("from", "auto@keycloak.org");
        config.put("host", "localhost");
        config.put("port", "3025");
        adminRealmRep.setSmtpServer(config);

        List<String> eventListeners = new ArrayList<>();
        eventListeners.add(JBossLoggingEventListenerProviderFactory.ID);
        eventListeners.add(EventsListenerProviderFactory.PROVIDER_ID);
        adminRealmRep.setEventsListeners(eventListeners);

        testRealms.add(adminRealmRep);
    }

    @Before
    public void setRealm() {
        realm = adminClient.realm(REALM_NAME);
        realmId = realm.toRepresentation().getId();
    }

    // old testsuite expects this realm to be removed at the end of the test
    // not sure if it really matters
    @After
    public void after() {
        for (RealmRepresentation r : adminClient.realms().findAll()) {
            if (r.getRealm().equals(REALM_NAME)) {
                removeRealm(r);
            }
        }
    }

    // Taken from Keycloak class in old testsuite.
    // So, code in old testsuite calling this looks like Keycloak.loadJson(.....)
    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

}
