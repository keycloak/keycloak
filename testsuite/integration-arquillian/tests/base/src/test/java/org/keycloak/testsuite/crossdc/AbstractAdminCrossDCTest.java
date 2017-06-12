/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.crossdc;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.log.JBossLoggingEventListenerProviderFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.events.EventsListenerProviderFactory;
import org.keycloak.testsuite.util.TestCleanup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractAdminCrossDCTest extends AbstractCrossDCTest {

    protected static final String REALM_NAME = "admin-client-test";

    protected RealmResource realm;
    protected String realmId;


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        findTestApp(testRealm).setDirectAccessGrantsEnabled(true);
    }



    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);

        RealmRepresentation adminRealmRep = new RealmRepresentation();
        adminRealmRep.setId(REALM_NAME);
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

    @Override
    protected TestCleanup getCleanup() {
        return getCleanup(REALM_NAME);
    }
}
