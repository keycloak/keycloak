package org.keycloak.tests.authz;

import java.io.ByteArrayInputStream;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.util.crypto.AuthzClientCryptoProvider;
import org.keycloak.common.crypto.CryptoIntegration;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AuthzClientTest {

    @Test
    public void testCreateWithEnvVars() {
        CryptoIntegration.setProvider(new AuthzClientCryptoProvider());

        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class, () -> {
            AuthzClient.create(new ByteArrayInputStream(("{\n"
                    + "  \"realm\": \"${env.KEYCLOAK_REALM:test}\",\n"
                    + "  \"auth-server-url\": \"${env.KEYCLOAK_AUTH_SERVER:http://test}\",\n"
                    + "  \"ssl-required\": \"external\",\n"
                    + "  \"enable-cors\": true,\n"
                    + "  \"resource\": \"my-server\",\n"
                    + "  \"credentials\": {\n"
                    + "    \"secret\": \"${env.KEYCLOAK_SECRET}\"\n"
                    + "  },\n"
                    + "  \"confidential-port\": 0,\n"
                    + "  \"policy-enforcer\": {\n"
                    + "    \"enforcement-mode\": \"ENFORCING\"\n"
                    + "  }\n"
                    + "}").getBytes()));
        });

        MatcherAssert.assertThat(runtimeException.getMessage(), Matchers.containsString("Could not obtain configuration from server"));
    }
}
