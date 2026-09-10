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

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * @author aschwart
 */
@KeycloakIntegrationTest(config = EventMetricsProviderWithTagsTest.EventMetricsServerConfig.class)
public class EventMetricsProviderWithTagsTest {

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

            // this event is not recorded as a metric as the event is not listed in the configuration
            EventBuilder eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGOUT)
                    .client(CLIENT_ID)
                    .detail(Details.IDENTITY_PROVIDER, "IDENTITY_PROVIDER");
            eventBuilder.success();

            // this event is recorded as an error
            eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGIN)
                    .client(CLIENT_ID)
                    .detail(Details.IDENTITY_PROVIDER, "IDENTITY_PROVIDER");
            eventBuilder.error("ERROR");

            // this event is recorded with the special logic about not found clients
            eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.REFRESH_TOKEN)
                    .client(CLIENT_ID);
            eventBuilder.error(Errors.CLIENT_NOT_FOUND);

        });

        runOnServer.run(session -> {
            MatcherAssert.assertThat("Two metrics recorded",
                    Metrics.globalRegistry.find("keycloak.user").meters().size(), Matchers.equalTo(2));
            MatcherAssert.assertThat("Searching for login error metric",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "ERROR", "realm", realmName, "client.id", CLIENT_ID, "idp", "IDENTITY_PROVIDER").count() == 1);
            MatcherAssert.assertThat("Searching for refresh with unknown client",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "refresh_token", "error", "client_not_found", "realm", realmName, "client.id", "unknown", "idp", "").count() == 1);
        });
    }

    public static class EventMetricsServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("metrics-enabled", "true")
                    .option("event-metrics-user-enabled", "true")
                    .option("event-metrics-user-tags", "realm,idp,clientId")
                    .option("event-metrics-user-events", "login,refresh_token");
        }
    }

}
