import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.server.ProviderModule;
import org.keycloak.test.framework.server.KeycloakTestServerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;


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

    // In a separate configuration class we can change some Keycloak server configs and apply it by referencing it in
    // the KeycloakIntegrationTest annotation.
    // The definition of our supplier module is in the MyProviderModule class.
    public static class ServerConfig implements KeycloakTestServerConfig {
        @Override
        public Set<Class<? extends ProviderModule>> providerModules() {
            return Set.of(MyProviderModule.class);
        }
    }
}
