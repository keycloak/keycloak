package org.keycloak.tests.admin.tracing;

import org.keycloak.connections.httpclient.DefaultHttpClientFactory;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.quarkus.runtime.tracing.OTelHttpClientFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest(config = TracingTest.ServerConfigWithTracing.class)
public class TracingTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void defaultSettingsIsUsed() {
        runOnServer.run(session -> {
            var defaultFactory = session.getKeycloakSessionFactory().getProviderFactory(HttpClientProvider.class, "default");
            assertThat(defaultFactory, notNullValue());
            assertThat(defaultFactory instanceof OTelHttpClientFactory, is(false));
            assertThat(defaultFactory instanceof DefaultHttpClientFactory, is(true));

            var defaultConfig = ((DefaultHttpClientFactory) defaultFactory).getConfig();
            assertThat(defaultConfig, notNullValue());
            assertThat(defaultConfig.get("connection-ttl-millis"), is("1"));
            assertThat(defaultConfig.get("socket-timeout-millis"), is("2222"));

            var otelFactory = session.getKeycloakSessionFactory().getProviderFactory(HttpClientProvider.class);
            assertThat(otelFactory, notNullValue());
            assertThat(otelFactory instanceof OTelHttpClientFactory, is(true));

            var otelConfig = ((OTelHttpClientFactory) otelFactory).getConfig();
            assertThat(otelConfig.get("connection-ttl-millis"), is("1"));
            assertThat(otelConfig.get("socket-timeout-millis"), is("2222"));
        });
    }

    public static class ServerConfigWithTracing implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("tracing-enabled", "true")
                    .option("spi-connections-http-client-default-connection-ttl-millis", "1")
                    .option("spi-connections-http-client-default-socket-timeout-millis", "2222")
                    .option("spi-connections-http-client-opentelemetry-connection-ttl-millis", "2") // not accepted
                    .option("spi-connections-http-client-opentelemetry-socket-timeout-millis", "3333"); // not accepted
        }
    }
}
