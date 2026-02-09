package org.keycloak.test.examples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 *
 * @author <a href="mailto:svacek@redhat.com">Simon Vacek</a>
 */
@KeycloakIntegrationTest(config = MyCustomProviderTest.ServerConfig.class)
public class MyCustomProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void httpGetTest() {
        String url = realm.getBaseUrl();

        HttpUriRequest request = new HttpGet(url + "/custom-provider/hello");
        try {
            HttpResponse response = HttpClientBuilder.create().build().execute(request);
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode());

            String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            Assertions.assertEquals("Hello World!", content);
        } catch (IOException ignored) {}
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.testframework", "keycloak-test-framework-example-providers");
        }

    }
}
