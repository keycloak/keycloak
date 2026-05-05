package org.keycloak.tests.events;

import org.keycloak.events.Details;
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

@KeycloakIntegrationTest(config = EventMetricsProviderNoEmptyTagsTest.EventMetricsServerConfig.class)
public class EventMetricsProviderNoEmptyTagsTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private final static String CLIENT_ID = "CLIENT_ID";

    @Test
    public void shouldReplaceEmptyTagsWithUnknown() {
        String realmName = realm.getName();

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();

            EventBuilder eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGIN)
                    .client(CLIENT_ID);
            eventBuilder.success();
        });

        runOnServer.run(session -> {
            MatcherAssert.assertThat("Empty tags should be replaced with 'none'",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "none", "realm", realmName, "client.id", CLIENT_ID, "idp", "none").count(), Matchers.equalTo(1.0));
        });
    }

    @Test
    public void shouldKeepNonEmptyTagsUnchanged() {
        String realmName = realm.getName();

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();

            EventBuilder eventBuilder = new EventBuilder(realm, session);
            eventBuilder.event(EventType.LOGIN)
                    .client(CLIENT_ID)
                    .detail(Details.IDENTITY_PROVIDER, "my-idp");
            eventBuilder.error("my-error");
        });

        runOnServer.run(session -> {
            MatcherAssert.assertThat("Non-empty tags should remain unchanged",
                    Metrics.globalRegistry.counter("keycloak.user", "event", "login", "error", "my-error", "realm", realmName, "client.id", CLIENT_ID, "idp", "my-idp").count(), Matchers.equalTo(1.0));
        });
    }

    public static class EventMetricsServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("metrics-enabled", "true")
                    .option("event-metrics-user-enabled", "true")
                    .option("event-metrics-user-tags", "realm,idp,clientId")
                    .option("event-metrics-user-allow-empty-tags", "false");
        }
    }

}
