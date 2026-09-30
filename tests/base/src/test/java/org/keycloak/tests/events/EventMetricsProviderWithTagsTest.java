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
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    private final static String REAL_IDP_ALIAS = "my-real-idp";

    @BeforeEach
    @AfterEach
    public void clearMetrics() {
        runOnServer.run(session -> {
            Metrics.globalRegistry.find("keycloak.user").meters()
                    .forEach(Metrics.globalRegistry::remove);
        });
    }

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
            MatcherAssert.assertThat("Error event with non-existent IDP should have empty idp tag",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "ERROR", "realm", realmName, "client.id", CLIENT_ID, "idp", "").count() == 1);
            MatcherAssert.assertThat("Searching for refresh with unknown client",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "refresh_token", "error", "client_not_found", "realm", realmName, "client.id", "unknown", "idp", "").count() == 1);
        });
    }

    @Test
    public void userProvidedIdpAliasShouldNotAppearInMetrics() {
        String realmName = realm.getName();

        runOnServer.run(session -> {
            IdentityProviderModel idp = new IdentityProviderModel();
            idp.setAlias(REAL_IDP_ALIAS);
            idp.setProviderId("oidc");
            idp.setEnabled(true);
            session.identityProviders().create(idp);
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            EventBuilder eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGIN)
                    .client(CLIENT_ID)
                    .detail(Details.IDENTITY_PROVIDER, REAL_IDP_ALIAS);
            eventBuilder.success();

            // Error event with a real IDP
            eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGIN)
                    .client(CLIENT_ID)
                    .detail(Details.IDENTITY_PROVIDER, REAL_IDP_ALIAS);
            eventBuilder.error("some_error");

            // Error event with a fake, attacker-provided IDP alias
            eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGIN)
                    .client(CLIENT_ID)
                    .detail(Details.IDENTITY_PROVIDER, "attacker-provided-fake-idp");
            eventBuilder.error("identity_provider_not_found");
        });

        runOnServer.run(session -> {
            MatcherAssert.assertThat("Successful event with real IDP should have idp tag",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "",
                            "realm", realmName, "client.id", CLIENT_ID, "idp", REAL_IDP_ALIAS).count(),
                    Matchers.equalTo(1.0));

            MatcherAssert.assertThat("Error event with real IDP should have idp tag",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "some_error",
                            "realm", realmName, "client.id", CLIENT_ID, "idp", REAL_IDP_ALIAS).count(),
                    Matchers.equalTo(1.0));

            MatcherAssert.assertThat("Error event with fake IDP should have empty idp tag",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "identity_provider_not_found",
                            "realm", realmName, "client.id", CLIENT_ID, "idp", "").count(),
                    Matchers.equalTo(1.0));

            boolean attackerIdpPresent = Metrics.globalRegistry.find("keycloak.user").meters().stream()
                    .flatMap(m -> m.getId().getTags().stream())
                    .filter(tag -> "idp".equals(tag.getKey()))
                    .map(Tag::getValue)
                    .anyMatch("attacker-provided-fake-idp"::equals);
            MatcherAssert.assertThat("Attacker-provided IDP alias must not appear in any metric tag",
                    attackerIdpPresent, Matchers.equalTo(false));
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
