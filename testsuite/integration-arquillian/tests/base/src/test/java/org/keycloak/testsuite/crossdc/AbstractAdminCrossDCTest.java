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
import org.keycloak.common.util.Retry;
import org.keycloak.testsuite.arquillian.InfinispanStatistics;
import org.keycloak.testsuite.events.EventsListenerProviderFactory;
import org.keycloak.testsuite.util.TestCleanup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.hamcrest.Matcher;
import org.junit.Before;
import static org.junit.Assert.assertThat;
import org.keycloak.representations.idm.ClientRepresentation;

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
        log.debug("Configuring test realm '" + testRealm.getRealm() + "'. Enabling direct access grant.");
        ClientRepresentation testApp = findTestApp(testRealm);
        if (testApp == null) {
            throw new IllegalStateException("Couldn't find the 'test-app' within the realm '" + testRealm.getRealm() + "'");
        }
        testApp.setDirectAccessGrantsEnabled(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.debug("--DC: AbstractAdminCrossDCTest.addTestRealms - adding realm: " + REALM_NAME);
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
        log.debug("--DC: AbstractAdminCrossDCTest.setRealm");
        realm = adminClient.realm(REALM_NAME);
        realmId = realm.toRepresentation().getId();
    }

    @Override
    protected TestCleanup getCleanup() {
        return getCleanup(REALM_NAME);
    }

    protected <T extends Comparable> void assertSingleStatistics(InfinispanStatistics stats, String key, Runnable testedCode, Function<T, Matcher<? super T>> matcherOnOldStat) {
        stats.reset();

        T oldStat = (T) stats.getSingleStatistics(key);
        testedCode.run();

        Retry.execute(() -> {
            T newStat = (T) stats.getSingleStatistics(key);

            Matcher<? super T> matcherInstance = matcherOnOldStat.apply(oldStat);
            
            log.infof("assertSingleStatistics '%s' : oldStat: %s, newStat: %s", key, oldStat.toString(), newStat.toString());
            assertThat(newStat, matcherInstance);
        }, 50, 200);
    }

    protected void assertStatistics(InfinispanStatistics stats, Runnable testedCode, BiConsumer<Map<String, Object>, Map<String, Object>> assertionOnStats) {
        stats.reset();

        Map<String, Object> oldStat = stats.getStatistics();
        testedCode.run();

        Retry.execute(() -> {
            Map<String, Object> newStat = stats.getStatistics();
            assertionOnStats.accept(oldStat, newStat);
        }, 50, 200);
    }

}
