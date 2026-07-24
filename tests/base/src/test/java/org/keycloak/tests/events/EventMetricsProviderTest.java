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

package org.keycloak.tests.events;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import io.micrometer.core.instrument.Metrics;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * @author aschwart
 */
@KeycloakIntegrationTest(config = EventMetricsProviderTest.EventMetricsServerConfig.class)
public class EventMetricsProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private final static String CLIENT_ID = "CLIENT_ID";

    @Test
    public void shouldCountSingleEventWithTagsAndFilter() {
        String realmName = realm.getName();

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            EventBuilder eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGIN)
                            .client(CLIENT_ID);
            eventBuilder.success();
        });

        runOnServer.run(session -> {
            MatcherAssert.assertThat("Searching for metrics match in " + Metrics.globalRegistry.find("keycloak.user").meters(),
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "", "realm", realmName).count() == 1);
        });
    }

    public static class EventMetricsServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("metrics-enabled", "true")
                    .option("event-metrics-user-enabled", "true");
        }
    }

}
