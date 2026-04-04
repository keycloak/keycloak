package org.keycloak.tests.cors;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.services.cors.Cors;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = CustomCorsAllowedHeadersTest.CustomCorsAllowedHeadersServerConfig.class)
public class CustomCorsAllowedHeadersTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @Test
    public void testCustomAllowedHeaders() throws IOException {
        List<String> list;
        try (SimpleHttpResponse response = simpleHttp.doOptions(realm.getBaseUrl() + "/.well-known/openid-configuration").header("Origin", "https://something").asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            list = Arrays.stream(response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_HEADERS).split(", ")).map(String::trim).toList();
        }
        MatcherAssert.assertThat(list, Matchers.hasItems("uber-trace-id", "x-b3-traceid"));
    }

    public static class CustomCorsAllowedHeadersServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.spiOption("cors", "default", "allowed-headers", "uber-trace-id,x-b3-traceid");
        }
    }

}
