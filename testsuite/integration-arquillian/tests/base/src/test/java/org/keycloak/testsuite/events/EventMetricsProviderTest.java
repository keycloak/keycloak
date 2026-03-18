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

package org.keycloak.testsuite.events;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.containers.AbstractQuarkusDeployableContainer;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.RealmBuilder;

import io.micrometer.core.instrument.Metrics;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author aschwart
 */
@EnableFeature(Profile.Feature.USER_EVENT_METRICS)
public class EventMetricsProviderTest extends AbstractKeycloakTest {

    private final static String CLIENT_ID = "CLIENT_ID";

    private void enableUserEventMetrics(String tags, String events) {
        try {
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().stop();
            enableEventMetricsOptions(tags, events);
            suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer().start();
            reconnectAdminClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void enableEventMetricsOptions(String tags, String events) {
        if (suiteContext.getAuthServerInfo().isQuarkus()) {
            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            List<String> args = new ArrayList<>();
            args.add("--metrics-enabled=true");
            args.add("--event-metrics-user-enabled=true");
            if (tags != null) {
                args.add("--event-metrics-user-tags=" + tags);
            }
            if (events != null) {
                args.add("--event-metrics-user-events=" + events);
            }
            container.setAdditionalBuildArgs(args);
        }
        else {
            throw new RuntimeException("This test will only work on Quarkus as it uses Micrometer");
        }
    }

    private static void setConfigProperty(String name, String value) {
        if (value != null) {
            System.setProperty(name, value);
        }
        else {
            System.clearProperty(name);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realm = RealmBuilder.create().name(TEST);
        testRealms.add(realm.build());
    }

    @Test
    public void shouldCountSingleEventWithTagsAndFilter() {
        ContainerAssume.assumeAuthServerQuarkus();

        enableUserEventMetrics(null, null);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST);
            session.getContext().setRealm(realm);
            EventBuilder eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGIN)
                            .client(CLIENT_ID);
            eventBuilder.success();
        });

        testingClient.server().run(session -> {
            MatcherAssert.assertThat("Searching for metrics match in " + Metrics.globalRegistry.find("keycloak.user").meters(),
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "", "realm", TEST).count() == 1);
        });

        // Show all labels, but filter out events like logout@
        enableUserEventMetrics("realm,idp,clientId", "login,refresh_token");

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST);
            session.getContext().setRealm(realm);

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

        testingClient.server().run(session -> {
            MatcherAssert.assertThat("Two metrics recorded",
                    Metrics.globalRegistry.find("keycloak.user").meters().size(), Matchers.equalTo(2));
            MatcherAssert.assertThat("Searching for login error metric",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "ERROR", "realm", TEST, "client.id", CLIENT_ID, "idp", "IDENTITY_PROVIDER").count() == 1);
            MatcherAssert.assertThat("Searching for refresh with unknown client",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "refresh_token", "error", "client_not_found", "realm", TEST, "client.id", "unknown", "idp", "").count() == 1);
        });

    }

    @After
    public void resetHostnameSettings() {
        if (suiteContext.getAuthServerInfo().isQuarkus()) {
            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.resetConfiguration();
        } else {
            throw new RuntimeException("This test will only work on Quarkus as it uses Micrometer");
        }
    }

}
