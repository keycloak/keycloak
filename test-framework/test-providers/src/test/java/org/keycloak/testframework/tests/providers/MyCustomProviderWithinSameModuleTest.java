package org.keycloak.testframework.tests.providers;

import java.io.IOException;
import java.net.URL;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 *
 * @see MyCustomRealmResourceProvider
 * @see org.keycloak.testframework.tests.MyCustomProviderTest
 * @author <a href="mailto:svacek@redhat.com">Simon Vacek</a>
 */
@KeycloakIntegrationTest(config = MyCustomProviderWithinSameModuleTest.ServerConfig.class)
public class MyCustomProviderWithinSameModuleTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @Test
    public void httpGetTest() throws IOException {
        URL url = KeycloakUriBuilder.fromUri(realm.getBaseUrl()).path("/custom-provider/hello").build().toURL();

        String response = simpleHttp.doGet(url.toString()).header("Accept", "text/plain").asString();

        Assertions.assertEquals("Hello World!", response);
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependencyCurrentProject();
        }

    }
}
